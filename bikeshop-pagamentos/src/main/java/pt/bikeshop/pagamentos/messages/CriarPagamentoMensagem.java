package pt.bikeshop.pagamentos.messages;

import java.math.BigDecimal;
import java.util.UUID;

/** Pedido da encomenda: criar o pagamento pendente com o total a pagar. */
public record CriarPagamentoMensagem(UUID encomendaId, BigDecimal valor) {
}
