package pt.bikeshop.stock.publishers;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pt.bikeshop.stock.configuration.MQConfig;

/**
 * Envia mensagens para a exchange bikeshop.eventos. Dentro de uma transação, só envia depois do commit:
 * assim nunca se anuncia uma reserva que acabou por não ficar gravada.
 */
@Component
@RequiredArgsConstructor
public class StockPublisher {

    private final RabbitTemplate rabbit;

    public void publicar(String routingKey, Object mensagem) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    rabbit.convertAndSend(MQConfig.EXCHANGE, routingKey, mensagem);
                }
            });
        } else {
            rabbit.convertAndSend(MQConfig.EXCHANGE, routingKey, mensagem);
        }
    }
}
