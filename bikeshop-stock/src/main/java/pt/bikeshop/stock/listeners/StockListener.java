package pt.bikeshop.stock.listeners;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pt.bikeshop.stock.configuration.MQConfig;
import pt.bikeshop.stock.messages.EncomendaMensagem;
import pt.bikeshop.stock.messages.ReservarStockMensagem;
import pt.bikeshop.stock.service.StockService;

/** Consome os pedidos da encomenda (slide 05: @RabbitListener num @Component). */
@Component
@RequiredArgsConstructor
public class StockListener {

    private final StockService stock;

    @RabbitListener(queues = MQConfig.FILA_RESERVAR)
    public void reservar(ReservarStockMensagem mensagem) {
        stock.reservar(mensagem);
    }

    @RabbitListener(queues = MQConfig.FILA_CONFIRMAR)
    public void confirmar(EncomendaMensagem mensagem) {
        stock.confirmar(mensagem.encomendaId());
    }

    @RabbitListener(queues = MQConfig.FILA_LIBERTAR)
    public void libertar(EncomendaMensagem mensagem) {
        stock.libertar(mensagem.encomendaId());
    }
}
