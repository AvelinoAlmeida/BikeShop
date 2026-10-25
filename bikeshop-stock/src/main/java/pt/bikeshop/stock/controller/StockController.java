package pt.bikeshop.stock.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pt.bikeshop.stock.dto.ReservaDto;
import pt.bikeshop.stock.dto.StockDto;
import pt.bikeshop.stock.dto.StockPedido;
import pt.bikeshop.stock.service.StockService;

import java.util.List;
import java.util.UUID;

@Tag(name = "Stock e reservas")
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stock;

    @GetMapping("/landing")
    public String landing() {
        return "O bikeshop-stock está a funcionar.";
    }

    @GetMapping
    public List<StockDto> listar() {
        return stock.listar();
    }

    @GetMapping("/{referencia}")
    public StockDto obter(@PathVariable String referencia) {
        return stock.obter(referencia);
    }

    @PutMapping("/{referencia}")
    public StockDto alterar(@PathVariable String referencia, @Valid @RequestBody StockPedido pedido) {
        return stock.alterarQuantidade(referencia, pedido.quantidade());
    }

    @GetMapping("/reservas/{encomendaId}")
    public List<ReservaDto> reservas(@PathVariable UUID encomendaId) {
        return stock.reservasDaEncomenda(encomendaId);
    }
}
