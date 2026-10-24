package pt.bikeshop.pagamentos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.bikeshop.pagamentos.entity.Pagamento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {

    Optional<Pagamento> findByEncomendaId(UUID encomendaId);

    List<Pagamento> findAllByOrderByCriadoEmDesc();
}
