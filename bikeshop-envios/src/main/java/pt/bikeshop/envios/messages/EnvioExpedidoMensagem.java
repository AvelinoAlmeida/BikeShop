package pt.bikeshop.envios.messages;

import java.util.UUID;

/** Resposta à encomenda: o envio saiu do armazém com este código de seguimento. */
public record EnvioExpedidoMensagem(UUID encomendaId, String codigo) {
}
