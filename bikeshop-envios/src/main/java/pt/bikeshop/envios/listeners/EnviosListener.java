package pt.bikeshop.envios.listeners;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pt.bikeshop.envios.configuration.MQConfig;
import pt.bikeshop.envios.messages.CriarEnvioMensagem;
import pt.bikeshop.envios.service.EnviosService;

@Component
@RequiredArgsConstructor
public class EnviosListener {

    private final EnviosService envios;

    @RabbitListener(queues = MQConfig.FILA_CRIAR)
    public void criar(CriarEnvioMensagem mensagem) {
        envios.criar(mensagem);
    }
}
