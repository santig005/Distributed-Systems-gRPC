package com.eafit.api_gateway.mom;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class RabbitMQService {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /* public void sendToQueue(String queueName, String message) {
        rabbitTemplate.convertAndSend(queueName, message);
    } */
   @PostConstruct
    public void init() {
        System.out.println("RabbitMQService inicializado correctamente");
    }
    public void sendToQueue(String queue_name,String message) {
        try {
            rabbitTemplate.convertAndSend(queue_name, message);
            System.out.println("Mensaje enviado a la cola: " + message);
        } catch (Exception e) {
            System.err.println("Error al enviar mensaje a RabbitMQ: " + e.getMessage());
        }
    }
     @Bean
    public Queue orderServiceQueue() {
        return new Queue("order-service-queue", true);
    }
    @Bean
    public Queue userServiceQueue() {
        return new Queue("user-service-queue", true);
    }
    @Bean
    public Queue productServiceQueue() {
        return new Queue("product-service-queue", true);
    }
}