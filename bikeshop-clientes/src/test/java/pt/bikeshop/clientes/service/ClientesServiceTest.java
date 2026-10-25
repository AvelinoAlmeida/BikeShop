package pt.bikeshop.clientes.service;

import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import pt.bikeshop.clientes.dto.ClienteDto;
import pt.bikeshop.clientes.dto.ClientePedido;
import pt.bikeshop.clientes.dto.RegistoPedido;
import pt.bikeshop.clientes.entity.Cliente;
import pt.bikeshop.clientes.repository.ClienteRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClientesServiceTest {

    private final ClienteRepository clientes = mock(ClienteRepository.class);
    private final ClientesService servico = new ClientesService(clientes, new ModelMapper());

    @Test
    void registoDevolveOClienteQueJaExisteComAMorada() {
        Cliente rita = Cliente.builder().id(1L).nome("Rita Moreira").email("rita@bikeshop.pt")
                .morada("Rua das Flores 10").codigoPostal("4050-262").localidade("Porto").build();
        when(clientes.findByEmailIgnoreCase("Rita@BikeShop.pt")).thenReturn(Optional.of(rita));

        ClienteDto dto = servico.obterOuRegistar(new RegistoPedido("Rita", "Rita@BikeShop.pt"));

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getMoradaCompleta()).isEqualTo("Rua das Flores 10, 4050-262 Porto");
        verify(clientes, never()).save(any());
    }

    @Test
    void registoCriaOClienteNovoSemMorada() {
        when(clientes.findByEmailIgnoreCase("novo@bikeshop.pt")).thenReturn(Optional.empty());
        when(clientes.save(any())).thenAnswer(i -> i.getArgument(0));

        ClienteDto dto = servico.obterOuRegistar(new RegistoPedido("Novo Cliente", "novo@bikeshop.pt"));

        assertThat(dto.getNome()).isEqualTo("Novo Cliente");
        assertThat(dto.getMoradaCompleta()).isNull();
    }

    @Test
    void naoCriaDoisClientesComOMesmoEmail() {
        when(clientes.existsByEmailIgnoreCase("rita@bikeshop.pt")).thenReturn(true);

        assertThatThrownBy(() -> servico.criar(new ClientePedido("Rita", "rita@bikeshop.pt", null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rita@bikeshop.pt");
        verify(clientes, never()).save(any());
    }
}
