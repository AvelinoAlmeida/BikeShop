package pt.bikeshop.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.bikeshop.stock.entity.Stock;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByReferencia(String referencia);

    List<Stock> findAllByOrderByReferencia();
}
