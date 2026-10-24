package pt.bikeshop.catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.bikeshop.catalogo.entity.Peca;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PecaRepository extends JpaRepository<Peca, Long> {

    Optional<Peca> findByReferencia(String referencia);

    List<Peca> findAllByOrderByCategoriaCodigoAscPrecoAsc();

    /** Versão 2.0 (exercício do slide 06): as peças de uma marca, pelo nome do método. */
    List<Peca> findByMarcaIgnoreCaseOrderByPrecoAsc(String marca);

    /** Query própria em JPQL (slide 03): peças de uma categoria até um preço, da mais barata para a mais cara. */
    @Query("select p from Peca p where p.categoria.codigo = :categoria and p.preco <= :precoMax order by p.preco")
    List<Peca> procurarPorCategoriaAtePreco(@Param("categoria") String categoria, @Param("precoMax") BigDecimal precoMax);
}
