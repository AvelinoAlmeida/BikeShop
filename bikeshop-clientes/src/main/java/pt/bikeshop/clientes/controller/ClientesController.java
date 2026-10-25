package pt.bikeshop.clientes.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pt.bikeshop.clientes.dto.ClienteDto;
import pt.bikeshop.clientes.dto.ClientePedido;
import pt.bikeshop.clientes.dto.RegistoPedido;
import pt.bikeshop.clientes.service.ClientesService;

import java.util.List;

@Tag(name = "Clientes")
@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClientesController {

    private final ClientesService clientes;

    @GetMapping("/landing")
    public String landing() {
        return "O bikeshop-clientes está a funcionar.";
    }

    @GetMapping
    public List<ClienteDto> listar() {
        return clientes.listar();
    }

    @GetMapping("/{id}")
    public ClienteDto obter(@PathVariable Long id) {
        return clientes.obter(id);
    }

    @GetMapping("/email/{email}")
    public ClienteDto porEmail(@PathVariable String email) {
        return clientes.porEmail(email);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteDto criar(@Valid @RequestBody ClientePedido pedido) {
        return clientes.criar(pedido);
    }

    @PutMapping("/{id}")
    public ClienteDto alterar(@PathVariable Long id, @Valid @RequestBody ClientePedido pedido) {
        return clientes.alterar(id, pedido);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable Long id) {
        clientes.apagar(id);
    }

    /** Usado pela encomenda (Feign): devolve o cliente com este email, ou regista-o se for novo. */
    @PostMapping("/registo")
    public ClienteDto obterOuRegistar(@Valid @RequestBody RegistoPedido pedido) {
        return clientes.obterOuRegistar(pedido);
    }
}
