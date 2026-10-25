package pt.bikeshop.encomendas.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.bikeshop.encomendas.clients.CatalogoClient;
import pt.bikeshop.encomendas.clients.ClientesClient;
import pt.bikeshop.encomendas.configuration.MQConfig;
import pt.bikeshop.encomendas.dto.EncomendaDto;
import pt.bikeshop.encomendas.dto.EncomendaPedido;
import pt.bikeshop.encomendas.entity.Encomenda;
import pt.bikeshop.encomendas.entity.LinhaEncomenda;
import pt.bikeshop.encomendas.exception.NaoEncontradoException;
import pt.bikeshop.encomendas.exception.ServicoIndisponivelException;
import pt.bikeshop.encomendas.messages.Mensagens;
import pt.bikeshop.encomendas.publishers.EncomendasPublisher;
import pt.bikeshop.encomendas.repository.EncomendaRepository;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Orquestração da SAGA:
 * criar -> stock.reservar -> (stock.reservado -> pagamento.criar | stock.insuficiente -> CANCELADA)
 * -> (pagamento.aprovado -> PAGA + stock.confirmar + envio.criar | pagamento.recusado -> CANCELADA + stock.libertar)
 * -> envio.expedido -> ENVIADA
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EncomendasService {

    private final EncomendaRepository encomendas;
    private final CatalogoClient catalogo;
    private final ClientesClient clientes;
    private final EncomendasPublisher publisher;
    private final ModelMapper mapper;

    @Transactional(readOnly = true)
    public List<EncomendaDto> listar() {
        return encomendas.findTop50ByOrderByCriadoEmDesc().stream().map(this::paraDto).toList();
    }

    @Transactional(readOnly = true)
    public EncomendaDto obter(UUID id) {
        return paraDto(procurar(id));
    }

    /**
     * Passo 1: grava a encomenda PENDENTE com os preços do catálogo e o cliente (registado nos clientes se for novo)
     * e pede a reserva do stock.
     */
    public EncomendaDto criar(EncomendaPedido pedido) {
        Encomenda encomenda = new Encomenda(pedido.clienteNome(), pedido.clienteEmail());
        for (EncomendaPedido.Linha linha : pedido.linhas()) {
            CatalogoClient.PecaCatalogo peca = pecaDoCatalogo(linha.referencia());
            encomenda.adicionarLinha(peca.referencia(), peca.nome(), peca.preco(), linha.quantidade());
        }
        ClientesClient.Cliente cliente = chamar("Os clientes estão indisponíveis; tente daqui a pouco.",
                () -> clientes.registar(new ClientesClient.Registo(pedido.clienteNome(), pedido.clienteEmail())));
        encomenda.setClienteId(cliente.id());
        encomenda.setMoradaEntrega(cliente.moradaCompleta());
        encomendas.save(encomenda);
        List<Mensagens.Linha> linhas = encomenda.getLinhas().stream()
                .map(l -> new Mensagens.Linha(l.getReferencia(), l.getQuantidade())).toList();
        publisher.publicar(MQConfig.STOCK_RESERVAR, new Mensagens.ReservarStock(encomenda.getId(), linhas));
        log.info("Encomenda {} criada ({} EUR): pedido de reserva enviado", encomenda.getId(), encomenda.getTotal());
        return paraDto(encomenda);
    }

    /** Passo 2a: stock reservado; pede o pagamento. */
    public void stockReservado(UUID id) {
        Encomenda encomenda = procurar(id);
        if (encomenda.marcarReservada()) {
            publisher.publicar(MQConfig.PAGAMENTO_CRIAR, new Mensagens.CriarPagamento(id, encomenda.getTotal()));
            log.info("Encomenda {} reservada: pagamento pedido", id);
        }
    }

    /** Passo 2b: não há stock; a encomenda termina aqui. */
    public void stockInsuficiente(UUID id, String motivo) {
        if (procurar(id).cancelar(motivo)) {
            log.info("Encomenda {} cancelada: {}", id, motivo);
        }
    }

    /** Passo 3a: pago; o stock dá baixa das unidades reservadas e os envios preparam a entrega. */
    public void pagamentoAprovado(UUID id) {
        Encomenda encomenda = procurar(id);
        if (encomenda.marcarPaga()) {
            publisher.publicar(MQConfig.STOCK_CONFIRMAR, new Mensagens.EncomendaId(id));
            int artigos = encomenda.getLinhas().stream().mapToInt(LinhaEncomenda::getQuantidade).sum();
            publisher.publicar(MQConfig.ENVIO_CRIAR, new Mensagens.CriarEnvio(id, encomenda.getClienteNome(),
                    encomenda.getClienteEmail(), encomenda.getMoradaEntrega(), artigos));
            log.info("Encomenda {} paga: envio pedido", id);
        }
    }

    /** Passo 4: o envio saiu do armazém. */
    public void envioExpedido(UUID id, String codigo) {
        if (procurar(id).marcarEnviada(codigo)) {
            log.info("Encomenda {} enviada ({})", id, codigo);
        }
    }

    /** Passo 3b: compensação; cancela e manda libertar o stock reservado. */
    public void pagamentoRecusado(UUID id, String motivo) {
        if (procurar(id).cancelar(motivo)) {
            publisher.publicar(MQConfig.STOCK_LIBERTAR, new Mensagens.EncomendaId(id));
            log.info("Encomenda {} cancelada ({}): stock a libertar", id, motivo);
        }
    }

    private CatalogoClient.PecaCatalogo pecaDoCatalogo(String referencia) {
        try {
            return chamar("O catálogo está indisponível; tente daqui a pouco.", () -> catalogo.peca(referencia));
        } catch (ServicoIndisponivelException e) {
            if (e.getCause() instanceof FeignException.NotFound) {
                throw new IllegalArgumentException("A peça " + referencia + " não existe no catálogo");
            }
            throw e;
        }
    }

    /**
     * Chamada Feign com circuit breaker: qualquer falha passa a 503 com a causa original.
     * O erro do Feign chega embrulhado em NoFallbackAvailableException.
     */
    private static <T> T chamar(String indisponivel, Supplier<T> chamada) {
        try {
            return chamada.get();
        } catch (RuntimeException e) {
            Throwable causa = e instanceof NoFallbackAvailableException && e.getCause() != null ? e.getCause() : e;
            throw new ServicoIndisponivelException(indisponivel, causa);
        }
    }

    private Encomenda procurar(UUID id) {
        return encomendas.findById(id).orElseThrow(() -> new NaoEncontradoException("Encomenda não encontrada: " + id));
    }

    private EncomendaDto paraDto(Encomenda encomenda) {
        return mapper.map(encomenda, EncomendaDto.class);
    }
}
