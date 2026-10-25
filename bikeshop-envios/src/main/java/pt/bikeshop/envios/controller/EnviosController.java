package pt.bikeshop.envios.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pt.bikeshop.envios.dto.EnvioDto;
import pt.bikeshop.envios.service.EnviosService;

import java.util.List;
import java.util.UUID;

@Tag(name = "Envios")
@RestController
@RequestMapping("/api/envios")
@RequiredArgsConstructor
public class EnviosController {

    private final EnviosService envios;

    @GetMapping("/landing")
    public String landing() {
        return "O bikeshop-envios está a funcionar.";
    }

    @GetMapping
    public List<EnvioDto> listar() {
        return envios.listar();
    }

    @GetMapping("/encomenda/{encomendaId}")
    public EnvioDto daEncomenda(@PathVariable UUID encomendaId) {
        return envios.daEncomenda(encomendaId);
    }

    @PostMapping("/encomenda/{encomendaId}/expedir")
    public EnvioDto expedir(@PathVariable UUID encomendaId) {
        return envios.expedir(encomendaId);
    }
}
