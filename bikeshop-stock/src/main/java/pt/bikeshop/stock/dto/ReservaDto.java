package pt.bikeshop.stock.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** Uma reserva vista de fora: sem o id interno da tabela (slide 04, DTO em vez da entidade). */
@Data
public class ReservaDto {
    private String referencia;
    private int quantidade;
    private String estado;
    private LocalDateTime criadaEm;
}
