package pt.bikeshop.clientes.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.bikeshop.clientes.dto.ClienteDto;
import pt.bikeshop.clientes.exception.NaoEncontradoException;
import pt.bikeshop.clientes.service.ClientesService;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientesController.class)
class ClientesControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ClientesService clientes;

    @Test
    void landingResponde() throws Exception {
        mvc.perform(get("/api/clientes/landing"))
                .andExpect(status().isOk())
                .andExpect(content().string("O bikeshop-clientes está a funcionar."));
    }

    @Test
    void criarResponde201() throws Exception {
        when(clientes.criar(any())).thenReturn(cliente());

        mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Rita Moreira\",\"email\":\"rita@bikeshop.pt\",\"codigoPostal\":\"4050-262\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("rita@bikeshop.pt"));
    }

    @Test
    void emailInvalidoDa400() throws Exception {
        mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Rita\",\"email\":\"rita\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(startsWith("email:")));
    }

    @Test
    void codigoPostalMalFormadoDa400() throws Exception {
        mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Rita\",\"email\":\"rita@bikeshop.pt\",\"codigoPostal\":\"4050\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("codigoPostal: deve ter o formato 0000-000"));
    }

    @Test
    void clienteInexistenteDa404ComONomeDoServico() throws Exception {
        when(clientes.obter(99L)).thenThrow(new NaoEncontradoException("Cliente não encontrado: 99"));

        mvc.perform(get("/api/clientes/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.servico").value("bikeshop-clientes"));
    }

    @Test
    void registoDevolveOCliente() throws Exception {
        when(clientes.obterOuRegistar(any())).thenReturn(cliente());

        mvc.perform(post("/api/clientes/registo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Rita Moreira\",\"email\":\"rita@bikeshop.pt\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moradaCompleta").value("Rua das Flores 10, 4050-262 Porto"));
    }

    private static ClienteDto cliente() {
        ClienteDto dto = new ClienteDto();
        dto.setId(1L);
        dto.setNome("Rita Moreira");
        dto.setEmail("rita@bikeshop.pt");
        dto.setMoradaCompleta("Rua das Flores 10, 4050-262 Porto");
        return dto;
    }
}
