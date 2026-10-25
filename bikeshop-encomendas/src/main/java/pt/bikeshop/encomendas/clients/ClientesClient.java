package pt.bikeshop.encomendas.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Clientes pelo nome no Eureka (bikeshop-clientes): devolve o cliente com o email, ou regista-o se for novo. */
@FeignClient(name = "bikeshop-clientes")
public interface ClientesClient {

    @PostMapping("/api/clientes/registo")
    Cliente registar(@RequestBody Registo registo);

    record Registo(String nome, String email) {
    }

    /** Só os campos de que a encomenda precisa; a morada vai para o envio. */
    record Cliente(Long id, String nome, String email, String moradaCompleta) {
    }
}
