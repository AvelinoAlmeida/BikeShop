package pt.bikeshop.catalogo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.bikeshop.catalogo.dto.PecaDto;
import pt.bikeshop.catalogo.exception.NaoEncontradoException;
import pt.bikeshop.catalogo.service.CatalogoService;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CatalogoController.class)
class CatalogoControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CatalogoService catalogo;

    @Test
    void landingResponde() throws Exception {
        mvc.perform(get("/api/pecas/landing"))
                .andExpect(status().isOk())
                .andExpect(content().string("O bikeshop-catalogo está a funcionar."));
    }

    @Test
    void devolveUmaPeca() throws Exception {
        PecaDto dto = new PecaDto();
        dto.setReferencia("BAT-500");
        dto.setPreco(new BigDecimal("649.00"));
        when(catalogo.obterPeca("BAT-500")).thenReturn(dto);

        mvc.perform(get("/api/pecas/BAT-500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referencia").value("BAT-500"))
                .andExpect(jsonPath("$.preco").value(649.00));
    }

    @Test
    void devolveAsPecasDeUmaMarca() throws Exception {
        PecaDto dto = new PecaDto();
        dto.setReferencia("BAT-500");
        dto.setMarca("Bosch");
        when(catalogo.listarPecasDaMarca("Bosch")).thenReturn(List.of(dto));

        mvc.perform(get("/api/pecas/marca/Bosch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].referencia").value("BAT-500"))
                .andExpect(jsonPath("$[0].marca").value("Bosch"));
    }

    @Test
    void pecaInexistenteDa404ComONomeDoServico() throws Exception {
        when(catalogo.obterPeca("NADA")).thenThrow(new NaoEncontradoException("Peça não encontrada: NADA"));

        mvc.perform(get("/api/pecas/NADA"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.servico").value("bikeshop-catalogo"));
    }

    @Test
    void pedidoSemPrecoDa400() throws Exception {
        mvc.perform(post("/api/pecas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"referencia\":\"X-1\",\"nome\":\"X\",\"categoriaCodigo\":\"BAT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.startsWith("preco:")));
    }
}
