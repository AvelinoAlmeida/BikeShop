package pt.bikeshop.encomendas.clients;

import com.sun.net.httpserver.HttpServer;
import feign.FeignException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import pt.bikeshop.encomendas.dto.EncomendaPedido;
import pt.bikeshop.encomendas.service.EncomendasService;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O CatalogoClient a sério (slide 04: testes com OpenFeign): o Feign faz o pedido HTTP e o LoadBalancer
 * reparte-o por duas instâncias falsas do catálogo, declaradas no discovery simples em vez do Eureka.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CatalogoClientTest {

    private static final AtomicInteger PEDIDOS_A = new AtomicInteger();
    private static final AtomicInteger PEDIDOS_B = new AtomicInteger();
    private static final HttpServer INSTANCIA_A = catalogoFalso(PEDIDOS_A);
    private static final HttpServer INSTANCIA_B = catalogoFalso(PEDIDOS_B);

    @DynamicPropertySource
    static void instancias(DynamicPropertyRegistry registo) {
        registo.add("spring.cloud.discovery.client.simple.instances.bikeshop-catalogo[0].uri", () -> url(INSTANCIA_A));
        registo.add("spring.cloud.discovery.client.simple.instances.bikeshop-catalogo[1].uri", () -> url(INSTANCIA_B));
    }

    @Autowired
    private CatalogoClient catalogo;

    @Autowired
    private EncomendasService servico;

    @BeforeEach
    void limparContagens() {
        PEDIDOS_A.set(0);
        PEDIDOS_B.set(0);
    }

    @AfterAll
    static void pararInstancias() {
        INSTANCIA_A.stop(0);
        INSTANCIA_B.stop(0);
    }

    @Test
    void lePecaDoCatalogoPeloJson() {
        CatalogoClient.PecaCatalogo peca = catalogo.peca("BAT-500");

        assertThat(peca.referencia()).isEqualTo("BAT-500");
        assertThat(peca.nome()).isEqualTo("PowerTube 500 Wh");
        assertThat(peca.preco()).isEqualByComparingTo(new BigDecimal("649.00"));
    }

    @Test
    void oLoadBalancerRepartePelasDuasInstancias() {
        for (int i = 0; i < 4; i++) {
            catalogo.peca("BAT-500");
        }

        assertThat(PEDIDOS_A.get()).isEqualTo(2);
        assertThat(PEDIDOS_B.get()).isEqualTo(2);
    }

    /** Com o circuit breaker, o 404 do Feign chega dentro de NoFallbackAvailableException. */
    @Test
    void pecaInexistenteDa404NoFeign() {
        assertThatThrownBy(() -> catalogo.peca("NAO-EXISTE"))
                .isInstanceOf(NoFallbackAvailableException.class)
                .hasCauseInstanceOf(FeignException.NotFound.class);
    }

    @Test
    void oServicoTransformaO404NumPedidoInvalido() {
        EncomendaPedido pedido = new EncomendaPedido("Ana", "ana@bikeshop.pt", List.of(new EncomendaPedido.Linha("NAO-EXISTE", 1)));

        assertThatThrownBy(() -> servico.criar(pedido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NAO-EXISTE");
    }

    /** Responde a GET /api/pecas/BAT-500 como o catálogo; qualquer outra referência dá 404. */
    private static HttpServer catalogoFalso(AtomicInteger pedidos) {
        try {
            HttpServer servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            servidor.createContext("/api/pecas/", troca -> {
                pedidos.incrementAndGet();
                boolean existe = troca.getRequestURI().getPath().endsWith("/BAT-500");
                byte[] corpo = (existe
                        ? "{\"referencia\":\"BAT-500\",\"nome\":\"PowerTube 500 Wh\",\"preco\":649.00}"
                        : "{\"servico\":\"bikeshop-catalogo\",\"estado\":404,\"mensagem\":\"Peça não encontrada\"}")
                        .getBytes(StandardCharsets.UTF_8);
                troca.getResponseHeaders().add("Content-Type", "application/json");
                troca.sendResponseHeaders(existe ? 200 : 404, corpo.length);
                troca.getResponseBody().write(corpo);
                troca.close();
            });
            servidor.start();
            return servidor;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String url(HttpServer servidor) {
        return "http://localhost:" + servidor.getAddress().getPort();
    }
}
