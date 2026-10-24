package pt.bikeshop.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.bikeshop.stock.entity.Reserva;

import java.util.List;
import java.util.UUID;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByEncomendaId(UUID encomendaId);

    List<Reserva> findByEncomendaIdAndEstado(UUID encomendaId, Reserva.Estado estado);
}
