package pt.bikeshop.clientes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.bikeshop.clientes.entity.Cliente;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<Cliente> findAllByOrderByNomeAsc();
}
