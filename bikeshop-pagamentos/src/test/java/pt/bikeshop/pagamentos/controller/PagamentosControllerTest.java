package pt.bikeshop.pagamentos.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.bikeshop.pagamentos.dto.PagamentoDto;
import pt.bikeshop.pagamentos.exception.NaoEncontradoException;
import pt.bikeshop.pagamentos.service.PagamentosService;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PagamentosController.class)
class PagamentosControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PagamentosService pagamentos;

    private final UUID encomenda = UUID.randomUUID();

    @Test
    void landingResponde() throws Exception {
        mvc.perform(get("/api/pagamentos/landing"))
                .andExpect(status().isOk())
                .andExpect(content().string("O bikeshop-pagamentos está a funcionar."));
    }

    @Test
    void devolveOPagamentoDaEncomenda() throws Exception {
        when(pagamentos.daEncomenda(encomenda)).thenReturn(pagamento("PENDENTE"));

        mvc.perform(get("/api/pagamentos/encomenda/{id}", encomenda))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDENTE"))
                .andExpect(jsonPath("$.valor").value(49.80));
    }

    @Test
    void encomendaSemPagamentoDa404ComONomeDoServico() throws Exception {
        when(pagamentos.daEncomenda(encomenda)).thenThrow(new NaoEncontradoException("Sem pagamento"));

        mvc.perform(get("/api/pagamentos/encomenda/{id}", encomenda))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.servico").value("bikeshop-pagamentos"));
    }

    @Test
    void aprovarDevolveOPagamentoAprovado() throws Exception {
        when(pagamentos.aprovar(encomenda)).thenReturn(pagamento("APROVADO"));

        mvc.perform(post("/api/pagamentos/encomenda/{id}/aprovar", encomenda))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROVADO"));
    }

    @Test
    void aprovarDuasVezesDa400() throws Exception {
        when(pagamentos.aprovar(encomenda)).thenThrow(new IllegalArgumentException("O pagamento já está APROVADO"));

        mvc.perform(post("/api/pagamentos/encomenda/{id}/aprovar", encomenda))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O pagamento já está APROVADO"));
    }

    @Test
    void identificadorInvalidoDa400() throws Exception {
        mvc.perform(get("/api/pagamentos/encomenda/nao-e-um-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.servico").value("bikeshop-pagamentos"));
    }

    private PagamentoDto pagamento(String estado) {
        PagamentoDto dto = new PagamentoDto();
        dto.setEncomendaId(encomenda);
        dto.setValor(new BigDecimal("49.80"));
        dto.setEstado(estado);
        return dto;
    }
}
