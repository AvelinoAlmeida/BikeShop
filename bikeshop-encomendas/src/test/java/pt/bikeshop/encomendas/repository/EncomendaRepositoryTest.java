package pt.bikeshop.encomendas.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import pt.bikeshop.encomendas.configuration.EncomendasConfig;
import pt.bikeshop.encomendas.entity.Encomenda;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(EncomendasConfig.class)
class EncomendaRepositoryTest {

    @Autowired
    private EncomendaRepository encomendas;

    @Autowired
    private TestEntityManager em;

    @Test
    void guardaAEncomendaComAsLinhas() {
        Encomenda e = new Encomenda("Ana", "ana@bikeshop.pt");
        e.adicionarLinha("BAT-500", "PowerTube 500 Wh", new BigDecimal("649.00"), 1);
        e.adicionarLinha("PNE-29", "Nobby Nic", new BigDecimal("64.90"), 2);
        encomendas.save(e);
        em.flush();
        em.clear();

        Encomenda lida = encomendas.findById(e.getId()).orElseThrow();

        assertThat(lida.getLinhas()).hasSize(2);
        assertThat(lida.getTotal()).isEqualByComparingTo("778.80");
        assertThat(lida.getCriadoEm()).isNotNull();
    }
}
