package pt.bikeshop.stock.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import pt.bikeshop.stock.configuration.MQConfig;
import pt.bikeshop.stock.entity.Reserva;
import pt.bikeshop.stock.entity.Stock;
import pt.bikeshop.stock.messages.ReservarStockMensagem;
import pt.bikeshop.stock.messages.ReservarStockMensagem.Linha;
import pt.bikeshop.stock.messages.RespostaStockMensagem;
import pt.bikeshop.stock.publishers.StockPublisher;
import pt.bikeshop.stock.repository.ReservaRepository;
import pt.bikeshop.stock.repository.StockRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** A parte do stock na SAGA: reservar tudo ou nada, repetir sem duplicar, confirmar e libertar (compensação). */
class StockServiceTest {

    private final StockRepository stocks = mock(StockRepository.class);
    private final ReservaRepository reservas = mock(ReservaRepository.class);
    private final StockPublisher publisher = mock(StockPublisher.class);
    private final StockService servico = new StockService(stocks, reservas, publisher, new ModelMapper());

    private final UUID encomenda = UUID.randomUUID();
    private Stock bateria;
    private Stock motor;

    @BeforeEach
    void dados() {
        bateria = Stock.builder().referencia("BAT-500").quantidade(8).reservada(0).build();
        motor = Stock.builder().referencia("MOT-CX5").quantidade(1).reservada(0).build();
        when(stocks.findByReferencia("BAT-500")).thenReturn(Optional.of(bateria));
        when(stocks.findByReferencia("MOT-CX5")).thenReturn(Optional.of(motor));
        when(reservas.findByEncomendaId(encomenda)).thenReturn(List.of());
    }

    @Test
    void reservaTodasAsLinhasEResponde() {
        servico.reservar(new ReservarStockMensagem(encomenda, List.of(new Linha("BAT-500", 2), new Linha("MOT-CX5", 1))));

        assertThat(bateria.getReservada()).isEqualTo(2);
        assertThat(motor.getReservada()).isEqualTo(1);
        verify(reservas, times(2)).save(any(Reserva.class));
        verify(publisher).publicar(eq(MQConfig.RESERVADO), eq(new RespostaStockMensagem(encomenda, null)));
    }

    @Test
    void semStockNaoReservaNada() {
        servico.reservar(new ReservarStockMensagem(encomenda, List.of(new Linha("BAT-500", 2), new Linha("MOT-CX5", 3))));

        assertThat(bateria.getReservada()).isZero();
        verify(reservas, never()).save(any());
        verify(publisher).publicar(eq(MQConfig.INSUFICIENTE),
                argThat(m -> ((RespostaStockMensagem) m).motivo().contains("MOT-CX5")));
    }

    @Test
    void mensagemRepetidaNaoReservaOutraVez() {
        when(reservas.findByEncomendaId(encomenda)).thenReturn(List.of(new Reserva()));

        servico.reservar(new ReservarStockMensagem(encomenda, List.of(new Linha("BAT-500", 2))));

        assertThat(bateria.getReservada()).isZero();
        verify(publisher).publicar(eq(MQConfig.RESERVADO), any());
    }

    @Test
    void libertarDevolveAsUnidades() {
        bateria.setReservada(2);
        Reserva reserva = Reserva.builder().encomendaId(encomenda).referencia("BAT-500").quantidade(2).estado(Reserva.Estado.ATIVA).build();
        when(reservas.findByEncomendaIdAndEstado(encomenda, Reserva.Estado.ATIVA)).thenReturn(List.of(reserva));

        servico.libertar(encomenda);

        assertThat(bateria.getReservada()).isZero();
        assertThat(bateria.getQuantidade()).isEqualTo(8);
        assertThat(reserva.getEstado()).isEqualTo(Reserva.Estado.LIBERTADA);
    }

    @Test
    void confirmarTiraDoArmazem() {
        bateria.setReservada(2);
        Reserva reserva = Reserva.builder().encomendaId(encomenda).referencia("BAT-500").quantidade(2).estado(Reserva.Estado.ATIVA).build();
        when(reservas.findByEncomendaIdAndEstado(encomenda, Reserva.Estado.ATIVA)).thenReturn(List.of(reserva));

        servico.confirmar(encomenda);

        assertThat(bateria.getQuantidade()).isEqualTo(6);
        assertThat(bateria.getReservada()).isZero();
        assertThat(reserva.getEstado()).isEqualTo(Reserva.Estado.CONFIRMADA);
    }
}
