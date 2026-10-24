package pt.bikeshop.pagamentos.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** RabbitMQ dos pagamentos (slide 05): recebe pagamento.criar e publica pagamento.aprovado ou pagamento.recusado. */
@Configuration
public class MQConfig {

    public static final String EXCHANGE = "bikeshop.eventos";

    public static final String CRIAR = "pagamento.criar";
    public static final String FILA_CRIAR = "pagamentos.criar.fila";

    public static final String APROVADO = "pagamento.aprovado";
    public static final String RECUSADO = "pagamento.recusado";

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
        Jackson2JsonMessageConverter conversor = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper tipos = new DefaultJackson2JavaTypeMapper();
        tipos.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        conversor.setJavaTypeMapper(tipos);
        return conversor;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
