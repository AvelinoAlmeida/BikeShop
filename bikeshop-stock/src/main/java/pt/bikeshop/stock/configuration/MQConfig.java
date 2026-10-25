package pt.bikeshop.stock.configuration;

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

/**
 * RabbitMQ do stock (slide 05): a exchange comum, as filas que este serviço consome e o conversor JSON.
 * O stock recebe os pedidos da encomenda (reservar, confirmar, libertar) e responde reservado ou insuficiente.
 */
@Configuration
public class MQConfig {

    public static final String EXCHANGE = "bikeshop.eventos";

    // O que o stock recebe
    public static final String RESERVAR = "stock.reservar";
    public static final String CONFIRMAR = "stock.confirmar";
    public static final String LIBERTAR = "stock.libertar";
    public static final String FILA_RESERVAR = "stock.reservar.fila";
    public static final String FILA_CONFIRMAR = "stock.confirmar.fila";
    public static final String FILA_LIBERTAR = "stock.libertar.fila";

    // O que o stock publica
    public static final String RESERVADO = "stock.reservado";
    public static final String INSUFICIENTE = "stock.insuficiente";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue filaReservar() {
        return new Queue(FILA_RESERVAR);
    }

    @Bean
    public Queue filaConfirmar() {
        return new Queue(FILA_CONFIRMAR);
    }

    @Bean
    public Queue filaLibertar() {
        return new Queue(FILA_LIBERTAR);
    }

    @Bean
    public Binding ligacaoReservar(Queue filaReservar, TopicExchange exchange) {
        return BindingBuilder.bind(filaReservar).to(exchange).with(RESERVAR);
    }

    @Bean
    public Binding ligacaoConfirmar(Queue filaConfirmar, TopicExchange exchange) {
        return BindingBuilder.bind(filaConfirmar).to(exchange).with(CONFIRMAR);
    }

    @Bean
    public Binding ligacaoLibertar(Queue filaLibertar, TopicExchange exchange) {
        return BindingBuilder.bind(filaLibertar).to(exchange).with(LIBERTAR);
    }

    /** JSON nas mensagens. O tipo vem do parâmetro do listener, porque cada serviço tem as suas classes. */
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
        return template;
    }
}
