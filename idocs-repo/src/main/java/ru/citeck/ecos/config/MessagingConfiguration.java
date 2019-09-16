package ru.citeck.ecos.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Properties;

/**
 * Messaging configuration
 */
@Configuration
public class MessagingConfiguration {

    /**
     * Properties constants
     */
    private static final String RABBIT_MQ_HOST = "rabbitmq.server.host";
    private static final String RABBIT_MQ_PORT= "rabbitmq.server.port";
    private static final String RABBIT_MQ_USERNAME= "rabbitmq.server.username";
    private static final String RABBIT_MQ_PASSWORD = "rabbitmq.server.password";
    private static final String BROKER_URL = "messaging.broker.url";

    /**
     * Global properties
     */
     @Autowired
     @Qualifier("global-properties")
     private Properties properties;

    /**
     * ActiveMQ connection factory
     * @return ActiveMQ connection factory
     */
    @Bean(name = "activeMQConnectionFactory")
    public ActiveMQConnectionFactory activeMQConnectionFactory() {
        if (properties.getProperty(BROKER_URL) != null) {
            ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
            activeMQConnectionFactory.setBrokerURL(properties.getProperty(BROKER_URL));
            return activeMQConnectionFactory;
        } else {
            return null;
        }
    }

    /**
     * JMS template
     * @param factory Connection factory
     * @return JMS template
     */
    @Bean(name = "jmsTemplate")
    public JmsTemplate jmsTemplate(ActiveMQConnectionFactory factory) {
        if (factory != null) {
            return new JmsTemplate(factory);
        } else {
            return null;
        }
    }

    /**
     * Connection factory bean
     * @return Connection factory or null (in case of absence "rabbitmq.server.host" global property)
     */
    @Bean(name = "historyRabbitConnectionFactory")
    public CachingConnectionFactory historyRabbitConnectionFactory() {
        if (properties.getProperty(RABBIT_MQ_HOST) != null) {
            CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
            connectionFactory.setHost(properties.getProperty(RABBIT_MQ_HOST));
            connectionFactory.setPort(Integer.valueOf(properties.getProperty(RABBIT_MQ_PORT)));
            connectionFactory.setUsername(properties.getProperty(RABBIT_MQ_USERNAME));
            connectionFactory.setPassword(properties.getProperty(RABBIT_MQ_PASSWORD));
            return connectionFactory;
        } else {
            return null;
        }
    }

    /**
     * Rabbit template bean
     * @param connectionFactory Connection factory
     * @return Rabbit template or null (in case of absence connection factory)
     */
    @Bean(name = "historyRabbitTemplate")
    public RabbitTemplate historyRabbitTemplate(@Qualifier("historyRabbitConnectionFactory") CachingConnectionFactory connectionFactory) {
        if (connectionFactory != null) {
            return new RabbitTemplate(connectionFactory);
        } else {
            return null;
        }
    }

    @Bean(name = "retryTemplate")
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(10000);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(6);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }
}
