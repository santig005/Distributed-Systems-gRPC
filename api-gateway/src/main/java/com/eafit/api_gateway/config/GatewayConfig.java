package com.eafit.api_gateway.config;

import com.eafit.api_gateway.mom.RabbitMQService;
import org.springframework.amqp.core.Queue;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import com.eafit.api_gateway.grpc.UserServiceClient;

@Configuration
public class GatewayConfig {

    @Autowired
    private UserServiceClient userServiceClient;
    @Autowired
    private RabbitMQService rabbitMQService;
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {

        /* return builder.routes()
                .route("user-service", r -> r.path("/api/users/**")
                        .uri("http://localhost:9090")) // Cambia por la URL real
                .build(); */


                /* return builder.routes()
                .route("user-service", r -> r.path("/api/users/**")
                        .filters(f -> f.filter((exchange, chain) -> {
                            // Simula una llamada al microservicio (aún no lo tienes, así que simulamos fallo)
                            boolean microserviceDown = true; // Cambia esto cuando integres gRPC
                            if (microserviceDown) {
                                String requestBody = "Solicitud para /api/users/";
                                rabbitMQService.sendToQueue(requestBody);
                                exchange.getResponse().setStatusCode(HttpStatus.ACCEPTED);
                                return exchange.getResponse().setComplete();
                            }
                            return chain.filter(exchange);
                        }))
                        .uri("http://localhost:9090"))
                .build(); */

                
                return builder.routes()
                .route("user-service", r -> r.path("/api/users/**")
                        .filters(f -> f.filter((exchange, chain) -> {
                            try {
                                String userId = exchange.getRequest().getPath().toString().split("/")[3]; // Ejemplo: /api/users/1
                                System.out.println("ID de usuario: " + userId);
                                String response = userServiceClient.getUser(userId);
                                exchange.getResponse().setStatusCode(HttpStatus.OK);
                                exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                                return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(response.getBytes())));
                            } catch (Exception e) {
                                String requestBody = "Solicitud para /api/users/";
                                rabbitMQService.sendToQueue(requestBody);
                                exchange.getResponse().setStatusCode(HttpStatus.ACCEPTED);
                                return exchange.getResponse().setComplete();
                            }
                        }))
                        .uri("http://localhost:9090"))
                .build();
    }
    @Bean
    public Queue userServiceQueue() {
        return new Queue("user-service-queue", true);
    }
}
