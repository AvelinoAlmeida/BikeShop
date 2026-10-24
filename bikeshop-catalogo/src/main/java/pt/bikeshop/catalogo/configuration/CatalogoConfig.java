package pt.bikeshop.catalogo.configuration;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** ModelMapper para converter entidades em DTO (slide 04) e auditoria JPA para as datas (slide 04). */
@Configuration
@EnableJpaAuditing
public class CatalogoConfig {

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}
