package pt.bikeshop.envios.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** RabbitMQ dos envios (slide 05): recebe envio.criar e publica envio.expedido. */
@Configuration
public class MQConfig {

    public static final String EXCHANGE = "bikeshop.eventos";

    public static final String CRIAR = "envio.criar";
    public static final String FILA_CRIAR = "envios.criar.fila";

    public static final String EXPEDIDO = "envio.expedido";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue filaCriar() {
        return new Queue(FILA_CRIAR);
    }

    @Bean
    public Binding ligacaoCriar(Queue filaCriar, TopicExchange exchange) {
        return BindingBuilder.bind(filaCriar).to(exchange).with(CRIAR);
    }

    @Bean
    public MessageConverter messageConverter() {
        JacksonJsonMessageConverter conversor = new JacksonJsonMessageConverter();
        DefaultJacksonJavaTypeMapper tipos = new DefaultJacksonJavaTypeMapper();
        tipos.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
        conversor.setJavaTypeMapper(tipos);
        return conversor;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setObservationEnabled(true);   // a mensagem leva o trace (tracing)
        return template;
    }
}
