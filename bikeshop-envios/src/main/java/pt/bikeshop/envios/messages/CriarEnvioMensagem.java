package pt.bikeshop.envios.messages;

import java.util.UUID;

/** Pedido da encomenda paga: preparar o envio para esta morada. */
public record CriarEnvioMensagem(UUID encomendaId, String clienteNome, String clienteEmail, String morada, int artigos) {
}
