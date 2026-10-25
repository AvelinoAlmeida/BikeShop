package pt.bikeshop.envios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Envios: cada encomenda paga fica com um envio em preparação até sair do armazém (expedido). */
@SpringBootApplication
public class EnviosApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnviosApplication.class, args);
    }
}
