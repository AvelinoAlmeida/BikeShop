package pt.bikeshop.gateway.configuration;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.observation.ClientRequestObservationContext;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;

@Configuration
public class GatewayConfig {

    /** Tracing sem ruído: as sondas do Actuator (Docker e Kubernetes) e os heartbeats do Eureka não geram traces. */
    @Bean
    public ObservationPredicate semSondasNemEureka() {
        return (nome, contexto) -> {
            if (contexto instanceof ServerRequestObservationContext servidor) {
                return !servidor.getCarrier().getPath().value().startsWith("/actuator");
            }
            if (contexto instanceof ClientRequestObservationContext cliente && cliente.getCarrier() != null) {
                return !cliente.getCarrier().getURI().getPath().startsWith("/eureka");
            }
            return true;
        };
    }
}
