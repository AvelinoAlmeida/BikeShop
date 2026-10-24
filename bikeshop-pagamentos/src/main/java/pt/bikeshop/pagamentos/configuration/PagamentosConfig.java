package pt.bikeshop.pagamentos.configuration;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class PagamentosConfig {

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}
