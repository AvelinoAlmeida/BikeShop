package pt.bikeshop.pagamentos.listeners;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pt.bikeshop.pagamentos.configuration.MQConfig;
import pt.bikeshop.pagamentos.messages.CriarPagamentoMensagem;
import pt.bikeshop.pagamentos.service.PagamentosService;

@Component
@RequiredArgsConstructor
public class PagamentosListener {

    private final PagamentosService pagamentos;

    @RabbitListener(queues = MQConfig.FILA_CRIAR)
    public void criar(CriarPagamentoMensagem mensagem) {
        pagamentos.criar(mensagem);
    }
}
