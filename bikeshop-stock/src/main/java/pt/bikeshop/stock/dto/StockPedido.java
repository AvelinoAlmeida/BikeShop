package pt.bikeshop.stock.dto;

import jakarta.validation.constraints.Min;

/** Nova quantidade em armazém de uma peça (repor ou acertar o stock). */
public record StockPedido(@Min(0) int quantidade) {
}
