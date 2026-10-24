package pt.bikeshop.encomendas.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Regras do agregado: total calculado pelas linhas e estados da SAGA só pela ordem certa. */
class EncomendaTest {

    @Test
    void totalEASomaDasLinhas() {
        Encomenda e = new Encomenda("Ana", "ana@bikeshop.pt");
        e.adicionarLinha("BAT-500", "PowerTube 500 Wh", new BigDecimal("649.00"), 1);
        e.adicionarLinha("TRV-PAS", "Pastilhas", new BigDecimal("24.90"), 2);

        assertThat(e.getTotal()).isEqualByComparingTo("698.80");
        assertThat(e.getLinhas()).hasSize(2);
    }

    @Test
    void quantidadeZeroNaoEntra() {
        Encomenda e = new Encomenda("Ana", "ana@bikeshop.pt");

        assertThatThrownBy(() -> e.adicionarLinha("BAT-500", "x", BigDecimal.TEN, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void caminhoFeliz() {
        Encomenda e = new Encomenda("Ana", "ana@bikeshop.pt");

        assertThat(e.marcarReservada()).isTrue();
        assertThat(e.marcarPaga()).isTrue();
        assertThat(e.getEstado()).isEqualTo(Encomenda.Estado.PAGA);
    }

    @Test
    void naoSePagaSemReserva() {
        Encomenda e = new Encomenda("Ana", "ana@bikeshop.pt");

        assertThat(e.marcarPaga()).isFalse();
        assertThat(e.getEstado()).isEqualTo(Encomenda.Estado.PENDENTE);
    }

    @Test
    void encomendaPagaNaoECancelada() {
        Encomenda e = new Encomenda("Ana", "ana@bikeshop.pt");
        e.marcarReservada();
        e.marcarPaga();

        assertThat(e.cancelar("tarde demais")).isFalse();
        assertThat(e.getEstado()).isEqualTo(Encomenda.Estado.PAGA);
    }

    @Test
    void reservaRepetidaNaoContaDuasVezes() {
        Encomenda e = new Encomenda("Ana", "ana@bikeshop.pt");

        assertThat(e.marcarReservada()).isTrue();
        assertThat(e.marcarReservada()).isFalse();
    }
}
