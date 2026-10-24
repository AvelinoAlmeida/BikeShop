package pt.bikeshop.stock.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import pt.bikeshop.stock.configuration.StockConfig;
import pt.bikeshop.stock.entity.Reserva;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(StockConfig.class)
class ReservaRepositoryTest {

    @Autowired
    private ReservaRepository reservas;

    @Test
    void procuraAsReservasAtivasDeUmaEncomenda() {
        UUID encomenda = UUID.randomUUID();
        reservas.save(reserva(encomenda, "BAT-500", Reserva.Estado.ATIVA));
        reservas.save(reserva(encomenda, "MOT-CX5", Reserva.Estado.LIBERTADA));
        reservas.save(reserva(UUID.randomUUID(), "BAT-500", Reserva.Estado.ATIVA));

        assertThat(reservas.findByEncomendaIdAndEstado(encomenda, Reserva.Estado.ATIVA))
                .extracting(Reserva::getReferencia).containsExactly("BAT-500");
        assertThat(reservas.findByEncomendaId(encomenda)).hasSize(2);
    }

    private static Reserva reserva(UUID encomenda, String referencia, Reserva.Estado estado) {
        return Reserva.builder().encomendaId(encomenda).referencia(referencia).quantidade(1).estado(estado).build();
    }
}
