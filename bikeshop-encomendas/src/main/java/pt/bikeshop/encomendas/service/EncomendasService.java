package pt.bikeshop.encomendas.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.bikeshop.encomendas.clients.CatalogoClient;
import pt.bikeshop.encomendas.configuration.MQConfig;
import pt.bikeshop.encomendas.dto.EncomendaDto;
import pt.bikeshop.encomendas.dto.EncomendaPedido;
import pt.bikeshop.encomendas.entity.Encomenda;
import pt.bikeshop.encomendas.exception.NaoEncontradoException;
import pt.bikeshop.encomendas.messages.Mensagens;
import pt.bikeshop.encomendas.publishers.EncomendasPublisher;
import pt.bikeshop.encomendas.repository.EncomendaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Orquestração da SAGA:
 * criar -> stock.reservar -> (stock.reservado -> pagamento.criar | stock.insuficiente -> CANCELADA)
 * -> (pagamento.aprovado -> PAGA + stock.confirmar | pagamento.recusado -> CANCELADA + stock.libertar)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EncomendasService {

    private final EncomendaRepository encomendas;
    private final CatalogoClient catalogo;
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

    /** Passo 1: grava a encomenda PENDENTE com os preços do catálogo e pede a reserva do stock. */
    public EncomendaDto criar(EncomendaPedido pedido) {
        Encomenda encomenda = new Encomenda(pedido.clienteNome(), pedido.clienteEmail());
        for (EncomendaPedido.Linha linha : pedido.linhas()) {
            CatalogoClient.PecaCatalogo peca = pecaDoCatalogo(linha.referencia());
            encomenda.adicionarLinha(peca.referencia(), peca.nome(), peca.preco(), linha.quantidade());
        }
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

    /** Passo 3a: pago; o stock dá baixa das unidades reservadas. */
    public void pagamentoAprovado(UUID id) {
        if (procurar(id).marcarPaga()) {
            publisher.publicar(MQConfig.STOCK_CONFIRMAR, new Mensagens.EncomendaId(id));
            log.info("Encomenda {} paga", id);
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
            return catalogo.peca(referencia);
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("A peça " + referencia + " não existe no catálogo");
        }
    }

    private Encomenda procurar(UUID id) {
        return encomendas.findById(id).orElseThrow(() -> new NaoEncontradoException("Encomenda não encontrada: " + id));
    }

    private EncomendaDto paraDto(Encomenda encomenda) {
        return mapper.map(encomenda, EncomendaDto.class);
    }
}
