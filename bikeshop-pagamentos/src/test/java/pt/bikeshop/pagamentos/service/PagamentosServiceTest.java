package pt.bikeshop.pagamentos.service;

import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import pt.bikeshop.pagamentos.configuration.MQConfig;
import pt.bikeshop.pagamentos.entity.Pagamento;
import pt.bikeshop.pagamentos.messages.CriarPagamentoMensagem;
import pt.bikeshop.pagamentos.messages.ResultadoPagamentoMensagem;
import pt.bikeshop.pagamentos.publishers.PagamentosPublisher;
import pt.bikeshop.pagamentos.repository.PagamentoRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PagamentosServiceTest {

    private final PagamentoRepository pagamentos = mock(PagamentoRepository.class);
    private final PagamentosPublisher publisher = mock(PagamentosPublisher.class);
    private final PagamentosService servico = new PagamentosService(pagamentos, publisher, new ModelMapper());
    private final UUID encomenda = UUID.randomUUID();

    @Test
    void criaOPagamentoPendente() {
        when(pagamentos.findByEncomendaId(encomenda)).thenReturn(Optional.empty());

        servico.criar(new CriarPagamentoMensagem(encomenda, new BigDecimal("649.00")));

        verify(pagamentos).save(argThat(p -> p.getEstado() == Pagamento.Estado.PENDENTE && p.getValor().equals(new BigDecimal("649.00"))));
    }

    @Test
    void mensagemRepetidaNaoCriaOutroPagamento() {
        when(pagamentos.findByEncomendaId(encomenda)).thenReturn(Optional.of(new Pagamento()));

        servico.criar(new CriarPagamentoMensagem(encomenda, BigDecimal.TEN));

        verify(pagamentos, never()).save(any());
    }

    @Test
    void aprovarPublicaPagamentoAprovado() {
        Pagamento pendente = Pagamento.builder().encomendaId(encomenda).valor(BigDecimal.TEN).estado(Pagamento.Estado.PENDENTE).build();
        when(pagamentos.findByEncomendaId(encomenda)).thenReturn(Optional.of(pendente));

        assertThat(servico.aprovar(encomenda).getEstado()).isEqualTo("APROVADO");
        verify(publisher).publicar(MQConfig.APROVADO, new ResultadoPagamentoMensagem(encomenda, null));
    }

    @Test
    void recusarPublicaPagamentoRecusado() {
        Pagamento pendente = Pagamento.builder().encomendaId(encomenda).valor(BigDecimal.TEN).estado(Pagamento.Estado.PENDENTE).build();
        when(pagamentos.findByEncomendaId(encomenda)).thenReturn(Optional.of(pendente));

        servico.recusar(encomenda);

        assertThat(pendente.getEstado()).isEqualTo(Pagamento.Estado.RECUSADO);
        verify(publisher).publicar(eq(MQConfig.RECUSADO), any(ResultadoPagamentoMensagem.class));
    }

    @Test
    void naoSePagaDuasVezes() {
        Pagamento aprovado = Pagamento.builder().encomendaId(encomenda).valor(BigDecimal.TEN).estado(Pagamento.Estado.APROVADO).build();
        when(pagamentos.findByEncomendaId(encomenda)).thenReturn(Optional.of(aprovado));

        assertThatThrownBy(() -> servico.aprovar(encomenda)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(publisher);
    }
}
