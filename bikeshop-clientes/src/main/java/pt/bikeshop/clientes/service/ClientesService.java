package pt.bikeshop.clientes.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.bikeshop.clientes.dto.ClienteDto;
import pt.bikeshop.clientes.dto.ClientePedido;
import pt.bikeshop.clientes.dto.RegistoPedido;
import pt.bikeshop.clientes.entity.Cliente;
import pt.bikeshop.clientes.exception.NaoEncontradoException;
import pt.bikeshop.clientes.repository.ClienteRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ClientesService {

    private final ClienteRepository clientes;
    private final ModelMapper mapper;

    @Transactional(readOnly = true)
    public List<ClienteDto> listar() {
        return clientes.findAllByOrderByNomeAsc().stream().map(this::paraDto).toList();
    }

    @Transactional(readOnly = true)
    public ClienteDto obter(Long id) {
        return paraDto(procurar(id));
    }

    @Transactional(readOnly = true)
    public ClienteDto porEmail(String email) {
        return paraDto(clientes.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NaoEncontradoException("Cliente não encontrado: " + email)));
    }

    public ClienteDto criar(ClientePedido pedido) {
        if (clientes.existsByEmailIgnoreCase(pedido.email())) {
            throw new IllegalArgumentException("Já existe um cliente com o email " + pedido.email());
        }
        Cliente cliente = new Cliente();
        copiar(pedido, cliente);
        return paraDto(clientes.save(cliente));
    }

    public ClienteDto alterar(Long id, ClientePedido pedido) {
        Cliente cliente = procurar(id);
        if (!cliente.getEmail().equalsIgnoreCase(pedido.email()) && clientes.existsByEmailIgnoreCase(pedido.email())) {
            throw new IllegalArgumentException("Já existe um cliente com o email " + pedido.email());
        }
        copiar(pedido, cliente);
        return paraDto(cliente);
    }

    public void apagar(Long id) {
        clientes.delete(procurar(id));
    }

    /** Chamado pela encomenda: devolve o cliente com este email ou regista-o, sem morada, se for novo. */
    public ClienteDto obterOuRegistar(RegistoPedido pedido) {
        return paraDto(clientes.findByEmailIgnoreCase(pedido.email()).orElseGet(() -> {
            log.info("Cliente novo registado pela encomenda: {}", pedido.email());
            return clientes.save(Cliente.builder().nome(pedido.nome()).email(pedido.email().toLowerCase()).build());
        }));
    }

    private void copiar(ClientePedido pedido, Cliente cliente) {
        cliente.setNome(pedido.nome());
        cliente.setEmail(pedido.email().toLowerCase());
        cliente.setTelefone(pedido.telefone());
        cliente.setMorada(pedido.morada());
        cliente.setCodigoPostal(pedido.codigoPostal());
        cliente.setLocalidade(pedido.localidade());
    }

    private Cliente procurar(Long id) {
        return clientes.findById(id).orElseThrow(() -> new NaoEncontradoException("Cliente não encontrado: " + id));
    }

    private ClienteDto paraDto(Cliente cliente) {
        ClienteDto dto = mapper.map(cliente, ClienteDto.class);
        dto.setMoradaCompleta(cliente.moradaCompleta());
        return dto;
    }
}
