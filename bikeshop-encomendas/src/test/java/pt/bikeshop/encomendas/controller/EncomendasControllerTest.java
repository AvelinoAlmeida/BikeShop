package pt.bikeshop.encomendas.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.bikeshop.encomendas.dto.EncomendaDto;
import pt.bikeshop.encomendas.exception.NaoEncontradoException;
import pt.bikeshop.encomendas.service.EncomendasService;

import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EncomendasController.class)
class EncomendasControllerTest {

    private static final String PEDIDO = """
            {"clienteNome":"Rita Moreira","clienteEmail":"rita@bikeshop.pt",
             "linhas":[{"referencia":"BAT-500","quantidade":1}]}""";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EncomendasService encomendas;

    @Test
    void landingResponde() throws Exception {
        mvc.perform(get("/api/encomendas/landing"))
                .andExpect(status().isOk())
                .andExpect(content().string("O bikeshop-encomendas está a funcionar."));
    }

    @Test
    void criarResponde202ComAEncomendaPendente() throws Exception {
        EncomendaDto dto = new EncomendaDto();
        dto.setId(UUID.randomUUID());
        dto.setEstado("PENDENTE");
        when(encomendas.criar(any())).thenReturn(dto);

        mvc.perform(post("/api/encomendas").contentType(MediaType.APPLICATION_JSON).content(PEDIDO))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.estado").value("PENDENTE"));
    }

    @Test
    void emailInvalidoDa400() throws Exception {
        mvc.perform(post("/api/encomendas").contentType(MediaType.APPLICATION_JSON)
                        .content(PEDIDO.replace("rita@bikeshop.pt", "nao-e-email")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(startsWith("clienteEmail:")));
    }

    @Test
    void encomendaSemLinhasDa400() throws Exception {
        mvc.perform(post("/api/encomendas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteNome\":\"Rita\",\"clienteEmail\":\"rita@bikeshop.pt\",\"linhas\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(startsWith("linhas:")));
    }

    @Test
    void jsonMalFormadoDa400() throws Exception {
        mvc.perform(post("/api/encomendas").contentType(MediaType.APPLICATION_JSON).content("{ isto nao e json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.servico").value("bikeshop-encomendas"));
    }

    @Test
    void encomendaInexistenteDa404ComONomeDoServico() throws Exception {
        UUID id = UUID.randomUUID();
        when(encomendas.obter(id)).thenThrow(new NaoEncontradoException("Encomenda não encontrada"));

        mvc.perform(get("/api/encomendas/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.servico").value("bikeshop-encomendas"));
    }
}
