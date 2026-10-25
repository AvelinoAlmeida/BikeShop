package pt.bikeshop.clientes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Clientes: quem compra e para onde se envia. A encomenda regista o cliente aqui antes de seguir. */
@SpringBootApplication
public class ClientesApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientesApplication.class, args);
    }
}
