package pt.bikeshop.registo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/** Registo de serviços: cada microsserviço regista-se aqui e a gateway encontra-os pelo nome. */
@SpringBootApplication
@EnableEurekaServer
public class RegistoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegistoApplication.class, args);
    }
}
