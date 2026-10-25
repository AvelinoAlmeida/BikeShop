package pt.bikeshop.pagamentos.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.List;

@Configuration
@EnableJpaAuditing
public class PagamentosConfig {

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    /** Documentação OpenAPI. O servidor é "/": a Swagger UI da gateway faz os pedidos pela própria gateway. */
    @Bean
    public OpenAPI documentacao() {
        return new OpenAPI()
                .info(new Info().title("bikeshop-pagamentos").description("Pagamentos das encomendas (simula a resposta do banco)").version("2.0"))
                .servers(List.of(new Server().url("/").description("Pela gateway")));
    }
}
