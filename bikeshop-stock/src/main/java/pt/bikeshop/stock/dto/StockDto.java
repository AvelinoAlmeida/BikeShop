package pt.bikeshop.stock.dto;

import lombok.Data;

@Data
public class StockDto {
    private String referencia;
    private int quantidade;
    private int reservada;
    private int livre;
}
