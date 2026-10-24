package pt.bikeshop.stock.messages;

import java.util.UUID;

/** Resposta à encomenda: stock.reservado (motivo vazio) ou stock.insuficiente (com o motivo). */
public record RespostaStockMensagem(UUID encomendaId, String motivo) {
}
