package ca.uhn.fhir.jpa.starter;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

	@Autowired
	private RabbitMQProperties rabbitMQProperties;

	@Bean
	public Queue imageQueue() {
		return new Queue(rabbitMQProperties.getQueue().getP2pImage().getName(), true);
	}

	@Bean
	public Queue documentQueue() {
		return new Queue(rabbitMQProperties.getQueue().getDocument().getName(), true);
	}

	@Bean
	public DirectExchange p2pDirectExchange() {
		return new DirectExchange(rabbitMQProperties.getExchange().getP2pExchange().getName());
	}

	@Bean
	public Binding imageBinding(Queue imageQueue, DirectExchange p2pDirectExchange) {
		String routingKey = rabbitMQProperties.getBinding().getP2pImage().getName();
		return BindingBuilder.bind(imageQueue).to(p2pDirectExchange).with(routingKey);
	}

	@Bean
	public Binding documentBinding(Queue documentQueue, DirectExchange p2pDirectExchange) {
		String routingKey = rabbitMQProperties.getBinding().getDocument().getName();
		return BindingBuilder.bind(documentQueue).to(p2pDirectExchange).with(routingKey);
	}

	@Bean
	public Queue emailQueue() {
		return new Queue(rabbitMQProperties.getQueue().getEmail().getName(), true);
	}

	@Bean
	public TopicExchange emailExchange() {
		return new TopicExchange(rabbitMQProperties.getExchange().getEmail().getName());
	}

	@Bean
	public Binding emailBinding(Queue emailQueue, TopicExchange emailExchange) {
		String routingKey = rabbitMQProperties.getBinding().getEmail().getName();
		return BindingBuilder.bind(emailQueue).to(emailExchange).with(routingKey);
	}


	// === SHARED TEMPLATE AND CONVERTER BEANS ===

	@Bean
	public RabbitTemplate amqpTemplate(ConnectionFactory connectionFactory) {
		RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		rabbitTemplate.setMessageConverter(converter());
		return rabbitTemplate;
	}

	@Bean
	public Jackson2JsonMessageConverter converter() {
		return new Jackson2JsonMessageConverter();
	}
}