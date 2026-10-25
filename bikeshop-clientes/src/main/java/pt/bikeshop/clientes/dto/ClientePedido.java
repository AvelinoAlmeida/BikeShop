package pt.bikeshop.clientes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Dados para criar ou alterar um cliente. A morada é opcional; o código postal tem o formato 0000-000. */
public record ClientePedido(
        @NotBlank @Size(max = 80) String nome,
        @NotBlank @Email String email,
        @Size(max = 20) String telefone,
        @Size(max = 120) String morada,
        @Pattern(regexp = "\\d{4}-\\d{3}", message = "deve ter o formato 0000-000") String codigoPostal,
        @Size(max = 60) String localidade) {
}
