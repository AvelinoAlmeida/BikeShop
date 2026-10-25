package pt.bikeshop.encomendas.clients;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import pt.bikeshop.encomendas.dto.EncomendaPedido;
import pt.bikeshop.encomendas.exception.ServicoIndisponivelException;
import pt.bikeshop.encomendas.service.EncomendasService;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Catálogo em baixo (Resilience4j): o catálogo está registado numa porta onde ninguém responde.
 * A encomenda dá 503 e, depois de 5 falhas, o circuito abre e as chamadas falham logo, sem tentar a rede.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CatalogoIndisponivelTest {

    private static final int PORTA_SEM_CATALOGO = portaLivre();

    @DynamicPropertySource
    static void catalogoParado(DynamicPropertyRegistry registo) {
        registo.add("spring.cloud.discovery.client.simple.instances.bikeshop-catalogo[0].uri",
                () -> "http://localhost:" + PORTA_SEM_CATALOGO);
    }

    @Autowired
    private EncomendasService servico;

    @Test
    void catalogoEmBaixoDaServicoIndisponivelEDepoisAbreOCircuito() {
        EncomendaPedido pedido = new EncomendaPedido("Ana", "ana@bikeshop.pt", List.of(new EncomendaPedido.Linha("BAT-500", 1)));

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> servico.criar(pedido))
                    .isInstanceOf(ServicoIndisponivelException.class)
                    .hasMessageContaining("catálogo está indisponível");
        }

        assertThatThrownBy(() -> servico.criar(pedido))
                .isInstanceOf(ServicoIndisponivelException.class)
                .satisfies(e -> assertThat(e.getCause()).isInstanceOf(CallNotPermittedException.class));
    }

    /** Uma porta que estava livre: ligar-lhe dá "connection refused", como um catálogo parado. */
    private static int portaLivre() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
