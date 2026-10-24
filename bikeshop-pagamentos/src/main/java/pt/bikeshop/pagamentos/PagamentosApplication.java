package pt.bikeshop.pagamentos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Pagamentos simulados: cada encomenda com stock reservado fica com um pagamento pendente até ser aprovado ou recusado. */
@SpringBootApplication
public class PagamentosApplication {

    public static void main(String[] args) {
        SpringApplication.run(PagamentosApplication.class, args);
    }
}
