package pt.bikeshop.pagamentos.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.bikeshop.pagamentos.configuration.MQConfig;
import pt.bikeshop.pagamentos.dto.PagamentoDto;
import pt.bikeshop.pagamentos.entity.Pagamento;
import pt.bikeshop.pagamentos.exception.NaoEncontradoException;
import pt.bikeshop.pagamentos.messages.CriarPagamentoMensagem;
import pt.bikeshop.pagamentos.messages.ResultadoPagamentoMensagem;
import pt.bikeshop.pagamentos.publishers.PagamentosPublisher;
import pt.bikeshop.pagamentos.repository.PagamentoRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PagamentosService {

    private final PagamentoRepository pagamentos;
    private final PagamentosPublisher publisher;
    private final ModelMapper mapper;

    @Transactional(readOnly = true)
    public List<PagamentoDto> listar() {
        return pagamentos.findAllByOrderByCriadoEmDesc().stream().map(this::paraDto).toList();
    }

    @Transactional(readOnly = true)
    public PagamentoDto daEncomenda(UUID encomendaId) {
        return paraDto(procurar(encomendaId));
    }

    /** Cria o pagamento pendente. Uma mensagem repetida não cria um segundo pagamento. */
    public void criar(CriarPagamentoMensagem pedido) {
        if (pagamentos.findByEncomendaId(pedido.encomendaId()).isPresent()) {
            log.info("Encomenda {} já tem pagamento: mensagem repetida", pedido.encomendaId());
            return;
        }
        pagamentos.save(Pagamento.builder().encomendaId(pedido.encomendaId()).valor(pedido.valor())
                .estado(Pagamento.Estado.PENDENTE).build());
        log.info("Encomenda {}: pagamento de {} EUR pendente", pedido.encomendaId(), pedido.valor());
    }

    /** Simula a resposta do banco: aprova o pagamento e avisa a encomenda. */
    public PagamentoDto aprovar(UUID encomendaId) {
        Pagamento pagamento = pendente(encomendaId);
        pagamento.setEstado(Pagamento.Estado.APROVADO);
        publisher.publicar(MQConfig.APROVADO, new ResultadoPagamentoMensagem(encomendaId, null));
        return paraDto(pagamento);
    }

    /** Simula um cartão recusado: a encomenda é cancelada e o stock libertado (compensação). */
    public PagamentoDto recusar(UUID encomendaId) {
        Pagamento pagamento = pendente(encomendaId);
        pagamento.setEstado(Pagamento.Estado.RECUSADO);
        publisher.publicar(MQConfig.RECUSADO, new ResultadoPagamentoMensagem(encomendaId, "Pagamento recusado pelo banco (simulado)"));
        return paraDto(pagamento);
    }

    private Pagamento pendente(UUID encomendaId) {
        Pagamento pagamento = procurar(encomendaId);
        if (pagamento.getEstado() != Pagamento.Estado.PENDENTE) {
            throw new IllegalArgumentException("O pagamento já está " + pagamento.getEstado());
        }
        return pagamento;
    }

    private Pagamento procurar(UUID encomendaId) {
        return pagamentos.findByEncomendaId(encomendaId)
                .orElseThrow(() -> new NaoEncontradoException("Sem pagamento para a encomenda " + encomendaId));
    }

    private PagamentoDto paraDto(Pagamento pagamento) {
        return mapper.map(pagamento, PagamentoDto.class);
    }
}
