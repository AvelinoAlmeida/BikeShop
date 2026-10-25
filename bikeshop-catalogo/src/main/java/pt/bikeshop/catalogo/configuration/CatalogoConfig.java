package pt.bikeshop.catalogo.configuration;

import io.micrometer.observation.ObservationPredicate;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.http.client.observation.ClientRequestObservationContext;
import org.springframework.http.server.observation.ServerRequestObservationContext;

import java.util.List;

/** ModelMapper para converter entidades em DTO (slide 04) e auditoria JPA para as datas (slide 04). */
@Configuration
@EnableJpaAuditing
public class CatalogoConfig {

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    /** Documentação OpenAPI. O servidor é "/": a Swagger UI da gateway faz os pedidos pela própria gateway. */
    @Bean
    public OpenAPI documentacao() {
        return new OpenAPI()
                .info(new Info().title("bikeshop-catalogo").description("Categorias e peças para bicicletas elétricas").version("2.0"))
                .servers(List.of(new Server().url("/").description("Pela gateway")));
    }

    /** Tracing sem ruído: as sondas do Actuator (Docker e Kubernetes) e os heartbeats do Eureka não geram traces. */
    @Bean
    public ObservationPredicate semSondasNemEureka() {
        return (nome, contexto) -> {
            if (contexto instanceof ServerRequestObservationContext servidor) {
                return !servidor.getCarrier().getRequestURI().startsWith("/actuator");
            }
            if (contexto instanceof ClientRequestObservationContext cliente && cliente.getCarrier() != null) {
                return !cliente.getCarrier().getURI().getPath().startsWith("/eureka");
            }
            return true;
        };
    }
}
