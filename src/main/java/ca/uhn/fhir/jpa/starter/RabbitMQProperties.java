package ca.uhn.fhir.jpa.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rabbitmq")
public class RabbitMQProperties {
	private Queue queue;
	private Exchange exchange;
	private Binding binding;

	public static class Queue {
		private Email email;
		private P2pImage p2pImage;
		private Document document;

		public static class Email {
			private String name;
			public String getName() { return name; }
			public void setName(String name) { this.name = name; }
		}

		public static class P2pImage {
			private String name;
			public String getName() { return name; }
			public void setName(String name) { this.name = name; }
		}

		public static class Document {
			private String name;
			public String getName() { return name; }
			public void setName(String name) { this.name = name; }
		}

		// Getters and Setters
		public Email getEmail() { return email; }
		public void setEmail(Email email) { this.email = email; }
		public P2pImage getP2pImage() { return p2pImage; }
		public void setP2pImage(P2pImage p2pImage) { this.p2pImage = p2pImage; }
		public Document getDocument() { return document; }
		public void setDocument(Document document) { this.document = document; }
	}

	// --- Nested Exchange Properties ---
	public static class Exchange {
		private Email email;
		private P2pExchange p2pExchange;

		public static class Email {
			private String name;
			public String getName() { return name; }
			public void setName(String name) { this.name = name; }
		}

		public static class P2pExchange {
			private String name;
			public String getName() { return name; }
			public void setName(String name) { this.name = name; }
		}

		// Getters and Setters
		public Email getEmail() { return email; }
		public void setEmail(Email email) { this.email = email; }
		public P2pExchange getP2pExchange() { return p2pExchange; }
		public void setP2pExchange(P2pExchange p2pExchange) { this.p2pExchange = p2pExchange; }
	}

	// --- Nested Binding (Routing Key) Properties ---
	public static class Binding {
		private Email email;
		private P2pImage p2pImage;
		private Document document;

		public static class Email {
			private String name; // This holds the routing key
			public String getName() { return name; }
			public void setName(String name) { this.name = name; }
		}

		public static class P2pImage {
			private String name; // This holds the routing key
			public String getName() { return name; }
			public void setName(String name) { this.name = name; }
		}

		public static class Document {
			private String name; // This holds the routing key
			public String getName() { return name; }
			public void setName(String name) { this.name = name; }
		}

		// Getters and Setters
		public Email getEmail() { return email; }
		public void setEmail(Email email) { this.email = email; }
		public P2pImage getP2pImage() { return p2pImage; }
		public void setP2pImage(P2pImage p2pImage) { this.p2pImage = p2pImage; }
		public Document getDocument() { return document; }
		public void setDocument(Document document) { this.document = document; }
	}

	// --- Top-level Getters and Setters ---
	public Queue getQueue() { return queue; }
	public void setQueue(Queue queue) { this.queue = queue; }
	public Exchange getExchange() { return exchange; }
	public void setExchange(Exchange exchange) { this.exchange = exchange; }
	public Binding getBinding() { return binding; }
	public void setBinding(Binding binding) { this.binding = binding; }
}