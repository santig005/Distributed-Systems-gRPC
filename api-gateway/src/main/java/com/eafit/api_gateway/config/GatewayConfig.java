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
import com.eafit.api_gateway.grpc.ProductServiceClient;
import product.Product.ProductResponse;
import com.google.protobuf.util.JsonFormat;


@Configuration
public class GatewayConfig {

    @Autowired
    private UserServiceClient userServiceClient;
    @Autowired
    private ProductServiceClient productServiceClient;
    @Autowired
    private RabbitMQService rabbitMQService;

    private String errorJson = """
                        {
                        "status": "accepted",
                        "message": "El microservicio no está disponible en este momento. La solicitud ha sido encolada para su procesamiento."
                        }
                        """;

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
                                String requestBody = exchange.getRequest().getPath().toString();
                                rabbitMQService.sendToQueue("user-service-queue",requestBody);
                                System.out.println("se se envió a la cola de RabbitMQ: ");
                                
                                exchange.getResponse().setStatusCode(HttpStatus.ACCEPTED);
                                exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                                return exchange.getResponse().writeWith(
                                    Mono.just(exchange.getResponse().bufferFactory().wrap(errorJson.getBytes()))
                                );
                            }
                        }))
                        .uri("no://op"))
                        .route("product-service", r -> r.path("/api/products/**")
                        .filters(f -> f.filter((exchange, chain) -> {
                            try {//protoc --go_out=. --go-grpc_out=. libs/protobufs/users.proto
                                String productId = exchange.getRequest().getPath().toString().split("/")[3]; // Ejemplo: /api/products/1
                                System.out.println("ID de product: " + productId);
                                ProductResponse response = productServiceClient.getProduct(productId);
                               // Serializa el objeto a formato JSON
                                String responseJson = JsonFormat.printer().print(response);
                                
                                exchange.getResponse().setStatusCode(HttpStatus.OK);
                                exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                                return exchange.getResponse().writeWith(
                                    Mono.just(exchange.getResponse().bufferFactory().wrap(responseJson.getBytes()))
                                );
                            } catch (Exception e) {
                                String requestBody =exchange.getRequest().getPath().toString();
                                rabbitMQService.sendToQueue("product-service-queue",requestBody);
                                System.out.println("se se envió a la cola de RabbitMQ: ");
                                
                                exchange.getResponse().setStatusCode(HttpStatus.ACCEPTED);
                                exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                                return exchange.getResponse().writeWith(
                                    Mono.just(exchange.getResponse().bufferFactory().wrap(errorJson.getBytes()))
                                );
                            }
                        }))
                        .uri("no://op"))
                .build();
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
