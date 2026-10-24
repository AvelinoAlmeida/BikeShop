package pt.bikeshop.catalogo.service;

import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import pt.bikeshop.catalogo.dto.PecaDto;
import pt.bikeshop.catalogo.entity.Categoria;
import pt.bikeshop.catalogo.entity.Peca;
import pt.bikeshop.catalogo.repository.CategoriaRepository;
import pt.bikeshop.catalogo.repository.PecaRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogoServiceTest {

    private final PecaRepository pecas = mock(PecaRepository.class);
    private final CatalogoService servico = new CatalogoService(mock(CategoriaRepository.class), pecas, new ModelMapper());

    @Test
    void modelMapperPreencheACategoriaDoDto() {
        Categoria bat = Categoria.builder().codigo("BAT").nome("Baterias").build();
        Peca peca = Peca.builder().referencia("BAT-500").nome("PowerTube 500 Wh").preco(new BigDecimal("649.00")).categoria(bat).build();
        when(pecas.findByReferencia("BAT-500")).thenReturn(Optional.of(peca));

        PecaDto dto = servico.obterPeca("BAT-500");

        assertThat(dto.getNome()).isEqualTo("PowerTube 500 Wh");
        assertThat(dto.getCategoriaCodigo()).isEqualTo("BAT");
        assertThat(dto.getCategoriaNome()).isEqualTo("Baterias");
    }
}
