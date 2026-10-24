package pt.bikeshop.catalogo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pt.bikeshop.catalogo.dto.CategoriaDto;
import pt.bikeshop.catalogo.dto.PecaDto;
import pt.bikeshop.catalogo.dto.PecaPedido;
import pt.bikeshop.catalogo.service.CatalogoService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService catalogo;

    /** Confirma que o serviço responde (slide 04). */
    @GetMapping("/pecas/landing")
    public String landing() {
        return "O bikeshop-catalogo está a funcionar.";
    }

    @GetMapping("/categorias")
    public List<CategoriaDto> categorias() {
        return catalogo.listarCategorias();
    }

    /** Todas as peças, ou as de uma categoria até um preço máximo: /api/pecas?categoria=BAT&precoMax=600 */
    @GetMapping("/pecas")
    public List<PecaDto> pecas(@RequestParam(required = false) String categoria,
                               @RequestParam(required = false) BigDecimal precoMax) {
        return catalogo.listarPecas(categoria, precoMax);
    }

    @GetMapping("/pecas/{referencia}")
    public PecaDto peca(@PathVariable String referencia) {
        return catalogo.obterPeca(referencia);
    }

    @PostMapping("/pecas")
    @ResponseStatus(HttpStatus.CREATED)
    public PecaDto criar(@Valid @RequestBody PecaPedido pedido) {
        return catalogo.criarPeca(pedido);
    }

    @PutMapping("/pecas/{referencia}")
    public PecaDto alterar(@PathVariable String referencia, @Valid @RequestBody PecaPedido pedido) {
        return catalogo.alterarPeca(referencia, pedido);
    }

    @DeleteMapping("/pecas/{referencia}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable String referencia) {
        catalogo.apagarPeca(referencia);
    }
}
