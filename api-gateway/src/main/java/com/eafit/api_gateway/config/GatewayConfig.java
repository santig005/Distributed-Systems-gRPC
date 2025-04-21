package com.eafit.api_gateway.config;

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
import order.Order.OrdersListResponse;
import product.Product.ProductResponse;
import usuario.Users.GetUserResponse;
import com.google.protobuf.util.JsonFormat;
import io.grpc.StatusRuntimeException;

import org.springframework.core.io.buffer.DataBufferUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.type.TypeReference;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class GatewayConfig {

    @Autowired private UserServiceClient userServiceClient;
    @Autowired private ProductServiceClient productServiceClient;
    @Autowired private OrderServiceClient orderServiceClient;
    @Autowired private GatewayGrpcErrorHandler errorHandler;
    @Autowired private ObjectMapper objectMapper;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
                return builder.routes()
                .route("user-service", r -> r.path("/api/users/**").and().method("GET")
                        .filters(f -> f.filter((exchange, chain) -> {
                            try {
                                String userId = exchange.getRequest().getPath().toString().split("/")[3];
                                System.out.println("GET /api/users - ID de usuario: " + userId);
                                if (userId == null || userId.trim().isEmpty()) {
                                    return errorHandler.writeErrorResponse(exchange, HttpStatus.BAD_REQUEST, "Se requiere ID de usuario.");
                                }

                                GetUserResponse response = userServiceClient.getUser(userId);
                                System.out.println("gRPC GetUser Response Status: " + response.getStatus());
                                if (!"success".equalsIgnoreCase(response.getStatus()) || response.getUsersCount() == 0) {
                                     return errorHandler.writeErrorResponse(exchange, HttpStatus.NOT_FOUND, "Usuario con ID " + userId + " no encontrado.");
                                }

                                String responseJson = JsonFormat.printer().print(response);
                                exchange.getResponse().setStatusCode(HttpStatus.OK);
                                exchange.getResponse().getHeaders().add("Content-Type", "application/json"); // Usar MediaType
                                return exchange.getResponse().writeWith(
                                        Mono.just(exchange.getResponse().bufferFactory().wrap(responseJson.getBytes(StandardCharsets.UTF_8)))
                                );
                            } catch (StatusRuntimeException e) {
                                System.err.println("gRPC Error en GetUser: " + e.getStatus());
                                // Delegar al manejador de errores
                                return errorHandler.handleGrpcError(exchange, e, "user-service-queue");
                            } catch (Exception e) {
                                System.err.println("Error inesperado en GetUser: " + e.getMessage());
                                return errorHandler.writeErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "Error interno procesando la solicitud de usuario.");
                            }
                        }))
                        .uri("no://op"))
                        .route("product-service", r -> r.path("/api/products/**").and().method("GET")
                        .filters(f -> f.filter((exchange, chain) -> {
                           try {
                                String productId = exchange.getRequest().getPath().toString().split("/")[3];
                                System.out.println("GET /api/products - ID de producto: " + productId);
                                if (productId == null || productId.trim().isEmpty()) {
                                    return errorHandler.writeErrorResponse(exchange, HttpStatus.BAD_REQUEST, "Se requiere ID de producto.");
                                }

                                ProductResponse response = productServiceClient.getProduct(productId);
                                String responseJson = JsonFormat.printer().print(response);

                                exchange.getResponse().setStatusCode(HttpStatus.OK);
                                exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                                return exchange.getResponse().writeWith(
                                        Mono.just(exchange.getResponse().bufferFactory().wrap(responseJson.getBytes(StandardCharsets.UTF_8)))
                                );
                            } catch (StatusRuntimeException e) {
                                System.err.println("gRPC Error en GetProduct: " + e.getStatus());
                                return errorHandler.handleGrpcError(exchange, e, "product-service-queue");
                            } catch (Exception e) {
                                 System.err.println("Error inesperado en GetProduct: " + e.getMessage());
                                return errorHandler.writeErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "Error interno procesando la solicitud de producto.");
                            }
                        }))
                        .uri("no://op"))
                        // --- Ruta Order Service (GET /api/orders/{id}) ---
                    .route("order-service-get", r -> r.path("/api/orders/**").and().method("GET")
                    .filters(f -> f.filter((exchange, chain) -> {
                        try {
                            String orderId = exchange.getRequest().getPath().toString().split("/")[3];
                            System.out.println("GET /api/orders - ID de pedido: " + orderId);
                            if (orderId == null || orderId.trim().isEmpty()) {
                                return errorHandler.writeErrorResponse(exchange, HttpStatus.BAD_REQUEST, "Se requiere ID de pedido.");
                            }

                            OrderResponse response = orderServiceClient.getOrder(orderId);
                            String responseJson = JsonFormat.printer().print(response);

                            exchange.getResponse().setStatusCode(HttpStatus.OK);
                            exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                            return exchange.getResponse().writeWith(
                                    Mono.just(exchange.getResponse().bufferFactory().wrap(responseJson.getBytes(StandardCharsets.UTF_8)))
                            );
                        } catch (StatusRuntimeException e) {
                            System.err.println("gRPC Error en GetOrder: " + e.getStatus());
                            return errorHandler.handleGrpcError(exchange, e, "order-service-queue");
                        } catch (Exception e) {
                            System.err.println("Error inesperado en GetOrder: " + e.getMessage());
                            return errorHandler.writeErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "Error interno procesando la solicitud de pedido.");
                        }
                    }))
                    .uri("no://op"))
                        .route("order-service-create", r -> r.path("/api/orders").and().method("POST")
                        .filters(f -> f.filter((exchange, chain) -> {
                            System.out.println("POST /api/orders - Recibiendo solicitud...");
                            return DataBufferUtils.join(exchange.getRequest().getBody())
                                .flatMap(dataBuffer -> {
                                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                    dataBuffer.read(bytes);
                                    DataBufferUtils.release(dataBuffer);
                                    String originalRequestBody = new String(bytes, StandardCharsets.UTF_8);
                                    System.out.println("Request Body: " + originalRequestBody);

                                    try {
                                        JsonNode json = objectMapper.readTree(originalRequestBody);
                                        String userId = json.path("userId").asText(null);
                                        List<String> productIds = null;
                                        if (json.hasNonNull("productIds") && json.get("productIds").isArray()) {
                                             productIds = objectMapper.convertValue(
                                                json.get("productIds"),
                                                new TypeReference<List<String>>() {}
                                            );
                                        }

                                        if (userId == null || productIds == null || productIds.isEmpty()) {
                                            System.err.println("Input inválido: userId o productIds faltante/vacío.");
                                            return errorHandler.writeErrorResponse(exchange, HttpStatus.BAD_REQUEST, "Se requiere 'userId' (string) y 'productIds' (array no vacío).");
                                        }

                                        System.out.println("Llamando a gRPC createOrder...");
                                        OrderResponse response = orderServiceClient.createOrder(userId, productIds);

                                        String responseJson = JsonFormat.printer().print(response);
                                        System.out.println("Response JSON to client: " + responseJson);

                                        exchange.getResponse().setStatusCode(HttpStatus.CREATED);
                                        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                                        return exchange.getResponse().writeWith(
                                                Mono.just(exchange.getResponse().bufferFactory().wrap(responseJson.getBytes(StandardCharsets.UTF_8)))
                                        );

                                    } catch (StatusRuntimeException e) {
                                        System.err.println("gRPC Error en CreateOrder: " + e.getStatus());
                                        // Delegar al manejador de errores, pasando el cuerpo original
                                        return errorHandler.handleGrpcError(exchange, e, "order-service-queue", originalRequestBody);
                                    } catch (Exception e) { // Captura errores de parsing JSON, etc.
                                        System.err.println("Error inesperado en CreateOrder (posiblemente JSON inválido): " + e.getMessage());
                                        return errorHandler.writeErrorResponse(exchange, HttpStatus.BAD_REQUEST, "Error procesando el cuerpo de la solicitud: " + e.getMessage());
                                    }
                                });
                        }))
                        .uri("no://op"))
                        .route("orders-by-user-id", r -> r.path("/api/orders-user/**").and().method("GET")
                    .filters(f -> f.filter((exchange, chain) -> {
                        try {
                            // Extraer userId de la ruta
                            String userId = exchange.getRequest().getPath().toString().split("/")[3];
                            System.out.println("GET /api/orders-user - ID de usuario: " + userId);
                            if (userId == null || userId.trim().isEmpty()) {
                                return errorHandler.writeErrorResponse(exchange, HttpStatus.BAD_REQUEST, "Se requiere ID de usuario.");
                            }
                            // Llamar al nuevo método del cliente gRPC
                            OrdersListResponse response = orderServiceClient.getOrdersByUserId(userId);

                            // Convertir la respuesta (que contiene la lista) a JSON
                            String responseJson = JsonFormat.printer().print(response);
                            System.out.println("Response JSON (" + response.getOrdersCount() + " orders) to client: " + responseJson);

                            exchange.getResponse().setStatusCode(HttpStatus.OK);
                            exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                            return exchange.getResponse().writeWith(
                                    Mono.just(exchange.getResponse().bufferFactory().wrap(responseJson.getBytes(StandardCharsets.UTF_8)))
                            );

                        } catch (StatusRuntimeException e) {
                            System.err.println("gRPC Error en GetOrdersByUserId: " + e.getStatus());
                            // Delegar al manejador de errores (no necesita encolado para GET)
                            return errorHandler.handleGrpcError(exchange, e, "order-service-queue"); // O pasar null como queue si no se quiere encolar nunca para GET
                        } catch (Exception e) {
                            System.err.println("Error inesperado en GetOrdersByUserId: " + e.getMessage());
                            return errorHandler.writeErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "Error interno procesando la solicitud de órdenes por usuario.");
                        }
                    }))
                    .uri("no://op")) 
                        .build();   
    } 
    
}
