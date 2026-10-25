package pt.bikeshop.encomendas.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pt.bikeshop.encomendas.dto.EncomendaDto;
import pt.bikeshop.encomendas.dto.EncomendaPedido;
import pt.bikeshop.encomendas.service.EncomendasService;

import java.util.List;
import java.util.UUID;

@Tag(name = "Encomendas")
@RestController
@RequestMapping("/api/encomendas")
@RequiredArgsConstructor
public class EncomendasController {

    private final EncomendasService encomendas;

    @GetMapping("/landing")
    public String landing() {
        return "O bikeshop-encomendas está a funcionar.";
    }

    @GetMapping
    public List<EncomendaDto> listar() {
        return encomendas.listar();
    }

    @GetMapping("/{id}")
    public EncomendaDto obter(@PathVariable UUID id) {
        return encomendas.obter(id);
    }

    /** Responde logo com a encomenda PENDENTE; o resto da SAGA corre por mensagens (consistência eventual). */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EncomendaDto criar(@Valid @RequestBody EncomendaPedido pedido) {
        return encomendas.criar(pedido);
    }
}
