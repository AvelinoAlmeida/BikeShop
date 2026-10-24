package pt.bikeshop.stock.messages;

import java.util.List;
import java.util.UUID;

/** Pedido da encomenda: reservar estas quantidades para a encomenda. */
public record ReservarStockMensagem(UUID encomendaId, List<Linha> linhas) {

    public record Linha(String referencia, int quantidade) {
    }
}
