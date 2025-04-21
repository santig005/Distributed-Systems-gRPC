package com.eafit.api_gateway.config; // O un paquete específico como com.eafit.api_gateway.error

import com.eafit.api_gateway.mom.RabbitMQService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType; // Usar MediaType
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component // Para que Spring lo detecte y pueda ser inyectado
public class GatewayGrpcErrorHandler {

    private final RabbitMQService rabbitMQService;
    private final ObjectMapper objectMapper;

    // Mensaje base para cuando SÍ encolamos
    private final String acceptedMessageJson = """
            {
              "status": "accepted",
              "message": "La solicitud ha sido aceptada y será procesada. Puede que el servicio subyacente no esté disponible temporalmente."
            }
            """;

    @Autowired // Inyección por constructor es preferible
    public GatewayGrpcErrorHandler(RabbitMQService rabbitMQService, ObjectMapper objectMapper) {
        this.rabbitMQService = rabbitMQService;
        this.objectMapper = objectMapper;
    }

    /**
     * Maneja StatusRuntimeException de gRPC para solicitudes GET (payload es la ruta).
     */
    public Mono<Void> handleGrpcError(ServerWebExchange exchange, StatusRuntimeException grpcException, String queueName) {
        String payloadToQueue = exchange.getRequest().getPath().toString();
        return handleGrpcErrorInternal(exchange, grpcException, queueName, payloadToQueue);
    }

    /**
     * Maneja StatusRuntimeException de gRPC para solicitudes POST/PUT (payload es el cuerpo).
     */
    public Mono<Void> handleGrpcError(ServerWebExchange exchange, StatusRuntimeException grpcException, String queueName, String payloadToQueue) {
        return handleGrpcErrorInternal(exchange, grpcException, queueName, payloadToQueue);
    }

    private Mono<Void> handleGrpcErrorInternal(ServerWebExchange exchange, StatusRuntimeException grpcException, String queueName, String payloadToQueue) {
        Status.Code grpcCode = grpcException.getStatus().getCode();
        String grpcMessage = grpcException.getStatus().getDescription(); // Mensaje del servicio JS

        boolean isRetryable = (grpcCode == Status.Code.UNAVAILABLE ||
                grpcCode == Status.Code.INTERNAL ||
                grpcCode == Status.Code.DEADLINE_EXCEEDED);

        if (isRetryable) {
            System.out.println("Error gRPC retryable (" + grpcCode + ") detectado. Encolando en '" + queueName + "'. Payload: " + payloadToQueue);
            try {
                rabbitMQService.sendToQueue(queueName, payloadToQueue);
                exchange.getResponse().setStatusCode(HttpStatus.ACCEPTED);
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON); // Usar MediaType
                return exchange.getResponse().writeWith(
                        Mono.just(exchange.getResponse().bufferFactory().wrap(acceptedMessageJson.getBytes(StandardCharsets.UTF_8)))
                );
            } catch (Exception mqException) {
                System.err.println("¡FALLO AL ENCOLAR EN RABBITMQ! " + mqException.getMessage());
                return writeErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor al intentar procesar la solicitud asíncronamente.");
            }
        } else {
            HttpStatus httpStatus = mapGrpcStatusToHttpStatus(grpcCode);
            System.out.println("Error gRPC no retryable (" + grpcCode + "). Respondiendo con HTTP " + httpStatus.value());
            return writeErrorResponse(exchange, httpStatus, grpcMessage); // Usar el mensaje del servicio JS
        }
    }

     /**
     * Mapea códigos de estado gRPC a códigos de estado HTTP.
     */
    public HttpStatus mapGrpcStatusToHttpStatus(Status.Code grpcCode) {
       // (Mismo código del switch que tenías antes)
        return switch (grpcCode) {
            case OK -> HttpStatus.OK;
            case CANCELLED -> HttpStatus.REQUEST_TIMEOUT;
            case UNKNOWN -> HttpStatus.INTERNAL_SERVER_ERROR;
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
            case DEADLINE_EXCEEDED -> HttpStatus.GATEWAY_TIMEOUT;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
            case RESOURCE_EXHAUSTED -> HttpStatus.TOO_MANY_REQUESTS;
            case FAILED_PRECONDITION -> HttpStatus.PRECONDITION_FAILED;
            case ABORTED -> HttpStatus.CONFLICT;
            case OUT_OF_RANGE -> HttpStatus.BAD_REQUEST;
            case UNIMPLEMENTED -> HttpStatus.NOT_IMPLEMENTED;
            case INTERNAL -> HttpStatus.INTERNAL_SERVER_ERROR;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case DATA_LOSS -> HttpStatus.INTERNAL_SERVER_ERROR;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    /**
     * Escribe una respuesta de error JSON estándar en el ServerWebExchange.
     */
    public Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON); // Usar MediaType

        ObjectNode errorJson = objectMapper.createObjectNode();
        errorJson.put("status", status.value());
        errorJson.put("error", status.getReasonPhrase());
        errorJson.put("message", message);

        byte[] responseBytes;
        try {
            responseBytes = objectMapper.writeValueAsBytes(errorJson);
        } catch (Exception e) {
            responseBytes = ("{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"Error creating error response\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(responseBytes))
        );
    }
}