package pt.bikeshop.clientes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** O que a encomenda envia: o cliente com este email, ou um novo com este nome se ainda não existir. */
public record RegistoPedido(@NotBlank String nome, @NotBlank @Email String email) {
}
