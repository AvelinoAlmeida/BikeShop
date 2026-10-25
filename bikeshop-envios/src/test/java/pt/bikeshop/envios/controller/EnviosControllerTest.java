package pt.bikeshop.envios.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.bikeshop.envios.dto.EnvioDto;
import pt.bikeshop.envios.exception.NaoEncontradoException;
import pt.bikeshop.envios.service.EnviosService;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnviosController.class)
class EnviosControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EnviosService envios;

    private final UUID encomenda = UUID.randomUUID();

    @Test
    void landingResponde() throws Exception {
        mvc.perform(get("/api/envios/landing"))
                .andExpect(status().isOk())
                .andExpect(content().string("O bikeshop-envios está a funcionar."));
    }

    @Test
    void devolveOEnvioDaEncomenda() throws Exception {
        when(envios.daEncomenda(encomenda)).thenReturn(envio("EM_PREPARACAO"));

        mvc.perform(get("/api/envios/encomenda/{id}", encomenda))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EM_PREPARACAO"))
                .andExpect(jsonPath("$.codigo").value("BS-1A2B3C4D"));
    }

    @Test
    void encomendaSemEnvioDa404ComONomeDoServico() throws Exception {
        when(envios.daEncomenda(encomenda)).thenThrow(new NaoEncontradoException("Sem envio"));

        mvc.perform(get("/api/envios/encomenda/{id}", encomenda))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.servico").value("bikeshop-envios"));
    }

    @Test
    void expedirDevolveOEnvioExpedido() throws Exception {
        when(envios.expedir(encomenda)).thenReturn(envio("EXPEDIDO"));

        mvc.perform(post("/api/envios/encomenda/{id}/expedir", encomenda))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EXPEDIDO"));
    }

    @Test
    void expedirDuasVezesDa400() throws Exception {
        when(envios.expedir(encomenda)).thenThrow(new IllegalArgumentException("O envio já está EXPEDIDO"));

        mvc.perform(post("/api/envios/encomenda/{id}/expedir", encomenda))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O envio já está EXPEDIDO"));
    }

    private EnvioDto envio(String estado) {
        EnvioDto dto = new EnvioDto();
        dto.setEncomendaId(encomenda);
        dto.setCodigo("BS-1A2B3C4D");
        dto.setEstado(estado);
        return dto;
    }
}
