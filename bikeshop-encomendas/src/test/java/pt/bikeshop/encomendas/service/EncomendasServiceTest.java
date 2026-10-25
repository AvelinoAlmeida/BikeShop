package pt.bikeshop.encomendas.service;

import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import pt.bikeshop.encomendas.clients.CatalogoClient;
import pt.bikeshop.encomendas.clients.ClientesClient;
import pt.bikeshop.encomendas.configuration.MQConfig;
import pt.bikeshop.encomendas.dto.EncomendaDto;
import pt.bikeshop.encomendas.dto.EncomendaPedido;
import pt.bikeshop.encomendas.entity.Encomenda;
import pt.bikeshop.encomendas.exception.ServicoIndisponivelException;
import pt.bikeshop.encomendas.messages.Mensagens;
import pt.bikeshop.encomendas.publishers.EncomendasPublisher;
import pt.bikeshop.encomendas.repository.EncomendaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** A orquestração da SAGA, com o catálogo, a base de dados e o RabbitMQ simulados. */
class EncomendasServiceTest {

    private final EncomendaRepository encomendas = mock(EncomendaRepository.class);
    private final CatalogoClient catalogo = mock(CatalogoClient.class);
    private final EncomendasPublisher publisher = mock(EncomendasPublisher.class);
    private final ClientesClient clientes = mock(ClientesClient.class);
    private final EncomendasService servico = new EncomendasService(encomendas, catalogo, clientes, publisher, new ModelMapper());

    @Test
    void criarUsaOPrecoDoCatalogoEPedeAReserva() {
        when(catalogo.peca("BAT-500")).thenReturn(new CatalogoClient.PecaCatalogo("BAT-500", "PowerTube 500 Wh", new BigDecimal("649.00")));
        when(clientes.registar(new ClientesClient.Registo("Ana", "ana@bikeshop.pt")))
                .thenReturn(new ClientesClient.Cliente(7L, "Ana", "ana@bikeshop.pt", "Rua Nova 1, 4000-001 Porto"));

        EncomendaDto dto = servico.criar(new EncomendaPedido("Ana", "ana@bikeshop.pt", List.of(new EncomendaPedido.Linha("BAT-500", 2))));

        assertThat(dto.getEstado()).isEqualTo("PENDENTE");
        assertThat(dto.getClienteId()).isEqualTo(7L);
        assertThat(dto.getMoradaEntrega()).isEqualTo("Rua Nova 1, 4000-001 Porto");
        assertThat(dto.getTotal()).isEqualByComparingTo("1298.00");
        assertThat(dto.getLinhas()).singleElement().satisfies(l -> assertThat(l.getNome()).isEqualTo("PowerTube 500 Wh"));
        verify(encomendas).save(any(Encomenda.class));
        verify(publisher).publicar(eq(MQConfig.STOCK_RESERVAR), any(Mensagens.ReservarStock.class));
    }

    @Test
    void pecaInexistenteNaoCriaEncomenda() {
        when(catalogo.peca("NADA")).thenThrow(mock(FeignException.NotFound.class));

        assertThatThrownBy(() -> servico.criar(new EncomendaPedido("Ana", "ana@bikeshop.pt", List.of(new EncomendaPedido.Linha("NADA", 1)))))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(publisher);
    }

    @Test
    void stockReservadoPedeOPagamentoUmaSoVez() {
        Encomenda e = encomenda();

        servico.stockReservado(e.getId());
        servico.stockReservado(e.getId());

        verify(publisher, times(1)).publicar(eq(MQConfig.PAGAMENTO_CRIAR), any(Mensagens.CriarPagamento.class));
        assertThat(e.getEstado()).isEqualTo(Encomenda.Estado.RESERVADA);
    }

    @Test
    void pagamentoRecusadoCancelaELibertaOStock() {
        Encomenda e = encomenda();
        e.marcarReservada();

        servico.pagamentoRecusado(e.getId(), "Cartão recusado");

        assertThat(e.getEstado()).isEqualTo(Encomenda.Estado.CANCELADA);
        assertThat(e.getMotivo()).isEqualTo("Cartão recusado");
        verify(publisher).publicar(eq(MQConfig.STOCK_LIBERTAR), any(Mensagens.EncomendaId.class));
    }

    @Test
    void clientesEmBaixoNaoCriaEncomenda() {
        when(catalogo.peca("BAT-500")).thenReturn(new CatalogoClient.PecaCatalogo("BAT-500", "PowerTube 500 Wh", new BigDecimal("649.00")));
        when(clientes.registar(any())).thenThrow(new IllegalStateException("ligação recusada"));

        assertThatThrownBy(() -> servico.criar(new EncomendaPedido("Ana", "ana@bikeshop.pt", List.of(new EncomendaPedido.Linha("BAT-500", 1)))))
                .isInstanceOf(ServicoIndisponivelException.class)
                .hasMessageContaining("clientes");
        verify(encomendas, never()).save(any());
        verifyNoInteractions(publisher);
    }

    @Test
    void pagamentoAprovadoConfirmaOStockEPedeOEnvio() {
        Encomenda e = encomenda();
        e.adicionarLinha("BAT-500", "PowerTube 500 Wh", new BigDecimal("649.00"), 2);
        e.setMoradaEntrega("Rua Nova 1, 4000-001 Porto");
        e.marcarReservada();

        servico.pagamentoAprovado(e.getId());

        assertThat(e.getEstado()).isEqualTo(Encomenda.Estado.PAGA);
        verify(publisher).publicar(eq(MQConfig.STOCK_CONFIRMAR), any(Mensagens.EncomendaId.class));
        verify(publisher).publicar(MQConfig.ENVIO_CRIAR,
                new Mensagens.CriarEnvio(e.getId(), "Ana", "ana@bikeshop.pt", "Rua Nova 1, 4000-001 Porto", 2));
    }

    @Test
    void envioExpedidoMarcaEnviadaComOCodigo() {
        Encomenda e = encomenda();
        e.marcarReservada();
        e.marcarPaga();

        servico.envioExpedido(e.getId(), "BS-1A2B3C4D");
        servico.envioExpedido(e.getId(), "BS-1A2B3C4D");

        assertThat(e.getEstado()).isEqualTo(Encomenda.Estado.ENVIADA);
        assertThat(e.getCodigoEnvio()).isEqualTo("BS-1A2B3C4D");
    }

    @Test
    void stockInsuficienteCancelaSemCompensar() {
        Encomenda e = encomenda();

        servico.stockInsuficiente(e.getId(), "Sem stock");

        assertThat(e.getEstado()).isEqualTo(Encomenda.Estado.CANCELADA);
        verifyNoInteractions(publisher);
    }

    /** Encomenda com id (o id normalmente vem do JPA), devolvida pelo repositório simulado. */
    private Encomenda encomenda() {
        Encomenda e = spy(new Encomenda("Ana", "ana@bikeshop.pt"));
        UUID id = UUID.randomUUID();
        doReturn(id).when(e).getId();
        when(encomendas.findById(id)).thenReturn(Optional.of(e));
        return e;
    }
}
