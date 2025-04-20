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
import com.eafit.api_gateway.grpc.OrderServiceClient;
import order.Order.OrderResponse;
import product.Product.ProductResponse;
import usuario.Users.GetUserResponse;
import com.google.protobuf.util.JsonFormat;

import org.springframework.core.io.buffer.DataBufferUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.type.TypeReference;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class GatewayConfig {

    @Autowired
    private UserServiceClient userServiceClient;
    @Autowired
    private ProductServiceClient productServiceClient;
    @Autowired
    private OrderServiceClient orderServiceClient;
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
                return builder.routes()
                .route("user-service", r -> r.path("/api/users/**").and().method("GET")
                        .filters(f -> f.filter((exchange, chain) -> {
                            try {
                                String userId = exchange.getRequest().getPath().toString().split("/")[3]; // Obtiene el ID del usuario de la ruta 
                                System.out.println("ID de usuario: " + userId);

                                
                                if(userId == null) {
                                    exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
                                    return exchange.getResponse().setComplete();
                                }
                                GetUserResponse response = userServiceClient.getUser(userId);
                                System.out.println("📦 gRPC user response proto object:");
                System.out.println(response.toString());

                String responseJson = JsonFormat.printer().print(response);
                System.out.println("🔸 Response JSON a enviar al cliente:");
                System.out.println(responseJson);
                // ————————————————————————————————
                                
                                exchange.getResponse().setStatusCode(HttpStatus.OK);
                                exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                                return exchange.getResponse().writeWith(
                                    Mono.just(exchange.getResponse().bufferFactory().wrap(responseJson.getBytes()))
                                );
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
                        .route("order-service", r -> r.path("/api/orders/**").and().method("GET")
                        .filters(f -> f.filter((exchange, chain) -> {
                            try {
                                String orderId = exchange.getRequest().getPath().toString().split("/")[3]; // Obtiene el ID del pedido de la ruta
                                System.out.println("ID de pedido: " + orderId);
                                if(orderId == null) {
                                    exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
                                    return exchange.getResponse().setComplete();
                                }
                                System.out.println("Vamos a llamar al servicio de pedidos...");
                                // Aquí deberías llamar al cliente gRPC para obtener la información del pedido
                                OrderResponse response = orderServiceClient.getOrder(orderId);
                                System.out.println("Respuesta del servicio de pedidos: " + response);
                                // Serializa el objeto a formato JSON
                                String responseJson = JsonFormat.printer().print(response);
                                

                                exchange.getResponse().setStatusCode(HttpStatus.OK);
                                exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                                return exchange.getResponse().writeWith(
                                    Mono.just(exchange.getResponse().bufferFactory().wrap(responseJson.getBytes()))
                                );
                            } catch (Exception e) {
                                String requestBody = exchange.getRequest().getPath().toString();
                                rabbitMQService.sendToQueue("order-service-queue",requestBody);
                                System.out.println("se se envió a la cola de RabbitMQ: ");
                                
                                exchange.getResponse().setStatusCode(HttpStatus.ACCEPTED);
                                exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                                return exchange.getResponse().writeWith(
                                    Mono.just(exchange.getResponse().bufferFactory().wrap(errorJson.getBytes()))
                                );
                            }
                        }))
                        .uri("no://op"))
                        .route("order-service-create", r -> r
                        .path("/api/orders").and().method("POST")
                        .filters(f -> f.filter((exchange, chain) -> {
                            System.out.println("entramos a post");
                            return DataBufferUtils.join(exchange.getRequest().getBody())
                            .flatMap(dataBuffer -> {
                                System.out.println("ya obtuvimos el cuerpo");
                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(bytes);
                                DataBufferUtils.release(dataBuffer);

                                try {
                                    System.out.println("entramos en el try");
                                    // 2) Parseamos JSON de entrada
                                    ObjectMapper mapper = new ObjectMapper();
                                    JsonNode json = mapper.readTree(bytes);
                                    String userId = json.get("userId").asText();
                                    List<String> productIds = mapper.convertValue(
                                        json.get("productIds"),
                                        new TypeReference<List<String>>() {}
                                    );
                            var response = orderServiceClient.createOrder(userId, productIds);

                            // ———————————  IMPRIME AQUÍ  ———————————
                            System.out.println("📦 gRPC createOrder response proto object:");
                            System.out.println(response.toString());

                            String responseJson = JsonFormat.printer().print(response);
                            System.out.println("🔸 Response JSON a enviar al cliente:");
                            System.out.println(responseJson);
                            // ——————————————————————————————————————————

                            
                            // 5) Respondemos con CREATED (201)
                            exchange.getResponse().setStatusCode(HttpStatus.CREATED);
                            exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                            return exchange.getResponse().writeWith(
                            Mono.just(exchange.getResponse()
                                .bufferFactory()
                                .wrap(responseJson.getBytes()))
                            ).then(Mono.fromRunnable(() -> {
                                System.out.println("Respuesta enviada exitosamente");
                            }));

                                } catch (Exception e) {
                                    // Encolamos la petición original y devolvemos accepted
                                    String body = new String(bytes, StandardCharsets.UTF_8);
                                    rabbitMQService.sendToQueue("order-service-queue", body);

                                    exchange.getResponse().setStatusCode(HttpStatus.ACCEPTED);
                                    exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                                    return exchange.getResponse().writeWith(
                                        Mono.just(exchange.getResponse()
                                        .bufferFactory()
                                        .wrap(errorJson.getBytes(StandardCharsets.UTF_8)))
                                    );
                                }
                            });
                        }))
                        .uri("no://op"))
                            .build();   
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
