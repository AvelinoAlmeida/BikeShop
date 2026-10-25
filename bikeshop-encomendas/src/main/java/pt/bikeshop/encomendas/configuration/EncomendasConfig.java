package pt.bikeshop.encomendas.configuration;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.modelmapper.ModelMapper;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableJpaAuditing
public class EncomendasConfig {

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    /**
     * Circuit breaker das chamadas ao catálogo: se metade de pelo menos 5 chamadas falhar, o circuito abre
     * e durante 10 s as encomendas falham logo, sem esperar pelo timeout. Uma peça inexistente (404)
     * é uma resposta válida do catálogo e não conta como falha.
     */
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> circuitBreakerCatalogo() {
        return fabrica -> fabrica.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .ignoreExceptions(FeignException.NotFound.class)
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(3)).build())
                .build());
    }

    /** Documentação OpenAPI. O servidor é "/": a Swagger UI da gateway faz os pedidos pela própria gateway. */
    @Bean
    public OpenAPI documentacao() {
        return new OpenAPI()
                .info(new Info().title("bikeshop-encomendas").description("Encomendas e SAGA entre o stock e os pagamentos").version("2.0"))
                .servers(List.of(new Server().url("/").description("Pela gateway")));
    }
}
