package pt.bikeshop.encomendas.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

/** Leitura do catálogo pelo nome no Eureka (bikeshop-catalogo); o LoadBalancer escolhe a instância. */
@FeignClient(name = "bikeshop-catalogo")
public interface CatalogoClient {

    @GetMapping("/api/pecas/{referencia}")
    PecaCatalogo peca(@PathVariable("referencia") String referencia);

    /** Só os campos de que a encomenda precisa. */
    record PecaCatalogo(String referencia, String nome, BigDecimal preco) {
    }
}
