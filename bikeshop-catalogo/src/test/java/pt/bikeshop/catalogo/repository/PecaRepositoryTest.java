package pt.bikeshop.catalogo.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import pt.bikeshop.catalogo.configuration.CatalogoConfig;
import pt.bikeshop.catalogo.entity.Categoria;
import pt.bikeshop.catalogo.entity.Peca;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(CatalogoConfig.class)
class PecaRepositoryTest {

    @Autowired
    private CategoriaRepository categorias;

    @Autowired
    private PecaRepository pecas;

    @Test
    void procuraPorCategoriaAtePrecoOrdenadoPorPreco() {
        Categoria bat = categorias.save(Categoria.builder().codigo("BAT").nome("Baterias").build());
        Categoria mot = categorias.save(Categoria.builder().codigo("MOT").nome("Motores").build());
        pecas.save(peca("BAT-750", "899.00", bat));
        pecas.save(peca("BAT-500", "649.00", bat));
        pecas.save(peca("BAT-900", "1200.00", bat));
        pecas.save(peca("MOT-CX5", "500.00", mot));

        List<Peca> resultado = pecas.procurarPorCategoriaAtePreco("BAT", new BigDecimal("900"));

        assertThat(resultado).extracting(Peca::getReferencia).containsExactly("BAT-500", "BAT-750");
    }

    @Test
    void auditoriaPreencheAsDatas() {
        Categoria bat = categorias.save(Categoria.builder().codigo("BAT").nome("Baterias").build());

        Peca guardada = pecas.saveAndFlush(peca("BAT-500", "649.00", bat));

        assertThat(guardada.getCriadoEm()).isNotNull();
        assertThat(guardada.getAlteradoEm()).isNotNull();
    }

    private static Peca peca(String referencia, String preco, Categoria categoria) {
        return Peca.builder().referencia(referencia).nome(referencia).preco(new BigDecimal(preco)).categoria(categoria).build();
    }
}
