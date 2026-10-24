package pt.bikeshop.encomendas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.bikeshop.encomendas.entity.Encomenda;

import java.util.List;
import java.util.UUID;

public interface EncomendaRepository extends JpaRepository<Encomenda, UUID> {

    List<Encomenda> findTop50ByOrderByCriadoEmDesc();
}
