package pt.bikeshop.envios.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.bikeshop.envios.entity.Envio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnvioRepository extends JpaRepository<Envio, UUID> {

    Optional<Envio> findByEncomendaId(UUID encomendaId);

    List<Envio> findAllByOrderByCriadoEmDesc();
}
