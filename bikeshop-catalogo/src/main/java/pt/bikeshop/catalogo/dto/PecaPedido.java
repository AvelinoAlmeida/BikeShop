package pt.bikeshop.catalogo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Corpo do pedido para criar ou alterar uma peça. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PecaPedido {

    @NotBlank
    private String referencia;

    @NotBlank
    private String nome;

    private String marca;
    private String descricao;
    private String compatibilidade;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal preco;

    @NotBlank
    private String categoriaCodigo;
}
