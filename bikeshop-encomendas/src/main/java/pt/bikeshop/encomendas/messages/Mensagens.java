package pt.bikeshop.encomendas.messages;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Mensagens trocadas na SAGA. Cada serviço tem as suas próprias classes com os mesmos campos:
 * o contrato é o JSON, não uma biblioteca partilhada.
 */
public final class Mensagens {

    private Mensagens() {
    }

    /** Para o stock: reservar estas quantidades. */
    public record ReservarStock(UUID encomendaId, List<Linha> linhas) {
    }

    public record Linha(String referencia, int quantidade) {
    }

    /** Para o stock: confirmar (encomenda paga) ou libertar (compensação). */
    public record EncomendaId(UUID encomendaId) {
    }

    /** Para os pagamentos: criar o pagamento pendente. */
    public record CriarPagamento(UUID encomendaId, BigDecimal valor) {
    }

    /** Resposta do stock ou dos pagamentos; motivo só quando correu mal. */
    public record Resposta(UUID encomendaId, String motivo) {
    }
}
