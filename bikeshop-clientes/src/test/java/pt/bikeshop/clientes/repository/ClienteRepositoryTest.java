package pt.bikeshop.clientes.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import pt.bikeshop.clientes.configuration.ClientesConfig;
import pt.bikeshop.clientes.entity.Cliente;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(ClientesConfig.class)
class ClienteRepositoryTest {

    @Autowired
    private ClienteRepository clientes;

    @Test
    void procuraPorEmailSemDistinguirMaiusculasEPreencheAsDatas() {
        clientes.saveAndFlush(Cliente.builder().nome("Rita Moreira").email("rita@bikeshop.pt").build());

        Cliente encontrado = clientes.findByEmailIgnoreCase("RITA@bikeshop.PT").orElseThrow();

        assertThat(encontrado.getNome()).isEqualTo("Rita Moreira");
        assertThat(encontrado.getCriadoEm()).isNotNull();
        assertThat(clientes.existsByEmailIgnoreCase("Rita@BikeShop.pt")).isTrue();
    }
}
