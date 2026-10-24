package pt.bikeshop.pagamentos.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class PagamentoDto {
    private UUID id;
    private UUID encomendaId;
    private BigDecimal valor;
    private String estado;
    private LocalDateTime criadoEm;
}
