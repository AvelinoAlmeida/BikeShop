package pt.bikeshop.clientes.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClienteDto {
    private Long id;
    private String nome;
    private String email;
    private String telefone;
    private String morada;
    private String codigoPostal;
    private String localidade;
    /** Morada numa linha (morada, código postal e localidade), como vai no envio. */
    private String moradaCompleta;
    private LocalDateTime criadoEm;
}
