package pt.bikeshop.catalogo.dto;

import lombok.Data;

import java.math.BigDecimal;

/** O que a API devolve de uma peça. O ModelMapper preenche categoriaCodigo a partir de categoria.codigo. */
@Data
public class PecaDto {
    private String referencia;
    private String nome;
    private String marca;
    private String descricao;
    private String compatibilidade;
    private BigDecimal preco;
    private String categoriaCodigo;
    private String categoriaNome;
}
