package pt.bikeshop.encomendas.listeners;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pt.bikeshop.encomendas.configuration.MQConfig;
import pt.bikeshop.encomendas.messages.Mensagens;
import pt.bikeshop.encomendas.service.EncomendasService;

/** As respostas do stock e dos pagamentos fazem avançar a SAGA. */
@Component
@RequiredArgsConstructor
public class EncomendasListener {

    private final EncomendasService encomendas;

    @RabbitListener(queues = MQConfig.FILA_STOCK_RESERVADO)
    public void stockReservado(Mensagens.Resposta resposta) {
        encomendas.stockReservado(resposta.encomendaId());
    }

    @RabbitListener(queues = MQConfig.FILA_STOCK_INSUFICIENTE)
    public void stockInsuficiente(Mensagens.Resposta resposta) {
        encomendas.stockInsuficiente(resposta.encomendaId(), resposta.motivo());
    }

    @RabbitListener(queues = MQConfig.FILA_PAGAMENTO_APROVADO)
    public void pagamentoAprovado(Mensagens.Resposta resposta) {
        encomendas.pagamentoAprovado(resposta.encomendaId());
    }

    @RabbitListener(queues = MQConfig.FILA_PAGAMENTO_RECUSADO)
    public void pagamentoRecusado(Mensagens.Resposta resposta) {
        encomendas.pagamentoRecusado(resposta.encomendaId(), resposta.motivo());
    }
}
