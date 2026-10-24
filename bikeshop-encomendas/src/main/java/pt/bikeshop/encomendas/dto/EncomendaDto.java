package pt.bikeshop.encomendas.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class EncomendaDto {
    private UUID id;
    private String clienteNome;
    private String clienteEmail;
    private String estado;
    private BigDecimal total;
    private String motivo;
    private LocalDateTime criadoEm;
    private List<LinhaDto> linhas;

    @Data
    public static class LinhaDto {
        private String referencia;
        private String nome;
        private BigDecimal precoUnitario;
        private int quantidade;
        private BigDecimal subtotal;
    }
}
