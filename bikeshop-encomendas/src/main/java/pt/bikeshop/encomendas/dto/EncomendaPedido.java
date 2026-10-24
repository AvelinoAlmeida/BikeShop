package pt.bikeshop.encomendas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** O que o cliente envia: quem é e que peças quer. O preço vem sempre do catálogo, nunca do pedido. */
public record EncomendaPedido(
        @NotBlank String clienteNome,
        @NotBlank @Email String clienteEmail,
        @NotEmpty List<@Valid Linha> linhas) {

    public record Linha(@NotBlank String referencia, @Min(1) int quantidade) {
    }
}
