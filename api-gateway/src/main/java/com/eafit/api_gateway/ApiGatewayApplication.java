package com.eafit.api_gateway;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import jakarta.annotation.PostConstruct;
@SpringBootApplication
public class ApiGatewayApplication {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @PostConstruct
    public void checkConnection() {
        try {
            rabbitTemplate.execute(channel -> {
                System.out.println("Conexión a RabbitMQ establecida correctamente");
                return null;
            });
        } catch (Exception e) {
            System.err.println("Error al conectar a RabbitMQ: " + e.getMessage());
        }
    }
}