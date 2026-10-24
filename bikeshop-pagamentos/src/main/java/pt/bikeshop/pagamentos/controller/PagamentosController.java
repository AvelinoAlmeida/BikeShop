package pt.bikeshop.pagamentos.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pt.bikeshop.pagamentos.dto.PagamentoDto;
import pt.bikeshop.pagamentos.service.PagamentosService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pagamentos")
@RequiredArgsConstructor
public class PagamentosController {

    private final PagamentosService pagamentos;

    @GetMapping("/landing")
    public String landing() {
        return "O bikeshop-pagamentos está a funcionar.";
    }

    @GetMapping
    public List<PagamentoDto> listar() {
        return pagamentos.listar();
    }

    @GetMapping("/encomenda/{encomendaId}")
    public PagamentoDto daEncomenda(@PathVariable UUID encomendaId) {
        return pagamentos.daEncomenda(encomendaId);
    }

    @PostMapping("/encomenda/{encomendaId}/aprovar")
    public PagamentoDto aprovar(@PathVariable UUID encomendaId) {
        return pagamentos.aprovar(encomendaId);
    }

    @PostMapping("/encomenda/{encomendaId}/recusar")
    public PagamentoDto recusar(@PathVariable UUID encomendaId) {
        return pagamentos.recusar(encomendaId);
    }
}
