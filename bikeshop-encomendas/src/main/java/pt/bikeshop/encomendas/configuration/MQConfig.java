package pt.bikeshop.encomendas.configuration;

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

/**
 * RabbitMQ das encomendas (slide 05). A encomenda é a orquestradora da SAGA:
 * envia pedidos ao stock e aos pagamentos e ouve as respostas de ambos.
 */
@Configuration
public class MQConfig {

    public static final String EXCHANGE = "bikeshop.eventos";

    // Pedidos que a encomenda envia
    public static final String STOCK_RESERVAR = "stock.reservar";
    public static final String STOCK_CONFIRMAR = "stock.confirmar";
    public static final String STOCK_LIBERTAR = "stock.libertar";
    public static final String PAGAMENTO_CRIAR = "pagamento.criar";

    // Respostas que a encomenda ouve
    public static final String STOCK_RESERVADO = "stock.reservado";
    public static final String STOCK_INSUFICIENTE = "stock.insuficiente";
    public static final String PAGAMENTO_APROVADO = "pagamento.aprovado";
    public static final String PAGAMENTO_RECUSADO = "pagamento.recusado";

    public static final String FILA_STOCK_RESERVADO = "encomendas.stock-reservado.fila";
    public static final String FILA_STOCK_INSUFICIENTE = "encomendas.stock-insuficiente.fila";
    public static final String FILA_PAGAMENTO_APROVADO = "encomendas.pagamento-aprovado.fila";
    public static final String FILA_PAGAMENTO_RECUSADO = "encomendas.pagamento-recusado.fila";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue filaStockReservado() {
        return new Queue(FILA_STOCK_RESERVADO);
    }

    @Bean
    public Queue filaStockInsuficiente() {
        return new Queue(FILA_STOCK_INSUFICIENTE);
    }

    @Bean
    public Queue filaPagamentoAprovado() {
        return new Queue(FILA_PAGAMENTO_APROVADO);
    }

    @Bean
    public Queue filaPagamentoRecusado() {
        return new Queue(FILA_PAGAMENTO_RECUSADO);
    }

    @Bean
    public Binding ligacaoStockReservado(Queue filaStockReservado, TopicExchange exchange) {
        return BindingBuilder.bind(filaStockReservado).to(exchange).with(STOCK_RESERVADO);
    }

    @Bean
    public Binding ligacaoStockInsuficiente(Queue filaStockInsuficiente, TopicExchange exchange) {
        return BindingBuilder.bind(filaStockInsuficiente).to(exchange).with(STOCK_INSUFICIENTE);
    }

    @Bean
    public Binding ligacaoPagamentoAprovado(Queue filaPagamentoAprovado, TopicExchange exchange) {
        return BindingBuilder.bind(filaPagamentoAprovado).to(exchange).with(PAGAMENTO_APROVADO);
    }

    @Bean
    public Binding ligacaoPagamentoRecusado(Queue filaPagamentoRecusado, TopicExchange exchange) {
        return BindingBuilder.bind(filaPagamentoRecusado).to(exchange).with(PAGAMENTO_RECUSADO);
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
