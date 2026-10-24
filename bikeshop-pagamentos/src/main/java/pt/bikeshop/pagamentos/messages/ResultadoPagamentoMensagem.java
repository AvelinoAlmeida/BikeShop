package pt.bikeshop.pagamentos.messages;

import java.util.UUID;

/** Resposta à encomenda: pagamento.aprovado (motivo vazio) ou pagamento.recusado (com o motivo). */
public record ResultadoPagamentoMensagem(UUID encomendaId, String motivo) {
}
