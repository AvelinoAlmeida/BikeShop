package pt.bikeshop.encomendas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/** Encomendas: recebe o pedido do cliente e conduz a SAGA (reservar stock, criar pagamento, confirmar ou compensar). */
@SpringBootApplication
@EnableFeignClients
public class EncomendasApplication {

    public static void main(String[] args) {
        SpringApplication.run(EncomendasApplication.class, args);
    }
}
