package pt.bikeshop.stock.messages;

import java.util.UUID;

/** Pedido da encomenda que só precisa do identificador: confirmar ou libertar as reservas. */
public record EncomendaMensagem(UUID encomendaId) {
}
