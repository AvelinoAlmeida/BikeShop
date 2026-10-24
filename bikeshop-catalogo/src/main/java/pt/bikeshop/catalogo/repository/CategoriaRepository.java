package pt.bikeshop.catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.bikeshop.catalogo.entity.Categoria;

import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    Optional<Categoria> findByCodigo(String codigo);
}
