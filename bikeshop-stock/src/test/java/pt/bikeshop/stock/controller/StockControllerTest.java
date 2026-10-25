package pt.bikeshop.stock.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.bikeshop.stock.dto.ReservaDto;
import pt.bikeshop.stock.dto.StockDto;
import pt.bikeshop.stock.exception.NaoEncontradoException;
import pt.bikeshop.stock.service.StockService;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockController.class)
class StockControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private StockService stock;

    @Test
    void landingResponde() throws Exception {
        mvc.perform(get("/api/stock/landing"))
                .andExpect(status().isOk())
                .andExpect(content().string("O bikeshop-stock está a funcionar."));
    }

    @Test
    void devolveOStockComAsUnidadesLivres() throws Exception {
        StockDto dto = new StockDto();
        dto.setReferencia("BAT-500");
        dto.setQuantidade(6);
        dto.setReservada(2);
        dto.setLivre(4);
        when(stock.obter("BAT-500")).thenReturn(dto);

        mvc.perform(get("/api/stock/BAT-500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.livre").value(4));
    }

    @Test
    void pecaSemStockDa404ComONomeDoServico() throws Exception {
        when(stock.obter("NADA")).thenThrow(new NaoEncontradoException("Sem stock para NADA"));

        mvc.perform(get("/api/stock/NADA"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.servico").value("bikeshop-stock"));
    }

    @Test
    void quantidadeNegativaDa400() throws Exception {
        mvc.perform(put("/api/stock/BAT-500").contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(startsWith("quantidade:")));
    }

    @Test
    void reservasSaoDtoSemOIdInterno() throws Exception {
        UUID encomenda = UUID.randomUUID();
        ReservaDto reserva = new ReservaDto();
        reserva.setReferencia("BAT-500");
        reserva.setQuantidade(1);
        reserva.setEstado("ATIVA");
        when(stock.reservasDaEncomenda(encomenda)).thenReturn(List.of(reserva));

        mvc.perform(get("/api/stock/reservas/{id}", encomenda))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("ATIVA"))
                .andExpect(jsonPath("$[0].id").doesNotExist());
    }
}
