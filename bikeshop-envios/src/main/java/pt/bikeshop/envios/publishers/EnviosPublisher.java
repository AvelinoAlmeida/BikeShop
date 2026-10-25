package pt.bikeshop.envios.publishers;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pt.bikeshop.envios.configuration.MQConfig;

/** Envia para a exchange bikeshop.eventos; dentro de uma transação, só depois do commit. */
@Component
@RequiredArgsConstructor
public class EnviosPublisher {

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
