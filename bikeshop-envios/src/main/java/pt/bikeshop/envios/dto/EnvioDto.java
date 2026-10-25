package pt.bikeshop.envios.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class EnvioDto {
    private UUID id;
    private UUID encomendaId;
    private String codigo;
    private String clienteNome;
    private String morada;
    private int artigos;
    private String estado;
    private LocalDateTime criadoEm;
}
