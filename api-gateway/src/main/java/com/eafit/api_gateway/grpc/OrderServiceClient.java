package com.eafit.api_gateway.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import order.OrderServiceGrpc;
import order.Order.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.concurrent.TimeUnit;
import java.util.List;

@Service
public class OrderServiceClient {

  @Value("${services.order.host}")
  private String orderServiceHost;

  @Value("${services.order.port}")
  private int orderServicePort;

  private OrderServiceGrpc.OrderServiceBlockingStub blockingStub;

  private ManagedChannel channel;

  @PostConstruct
  public void init() {
    this.channel = ManagedChannelBuilder.forAddress(orderServiceHost, orderServicePort)
        .usePlaintext() // Use TLS in production
        .build();
    blockingStub = OrderServiceGrpc.newBlockingStub(this.channel);
  }

  public OrderResponse createOrder(String userId, List<String> productIds) {
    OrderRequest request = OrderRequest.newBuilder()
        .setUserId(userId)
        .addAllProductIds(productIds)
        .build();
    return blockingStub.createOrder(request);
  }

  public OrderResponse getOrder(String orderId) {
    try {
      OrderByIdRequest request = OrderByIdRequest.newBuilder()
        .setId(orderId)
        .build();
      OrderResponse o= blockingStub.getOrder(request);
      return o;
    } catch (Exception e) {
      System.err.println("Error while fetching order: " + e.getMessage());
      e.printStackTrace();
      return null; // Handle this appropriately in your application
    }
  }
  public OrdersListResponse getOrdersByUserId(String userId) {
        OrdersByUserIdRequest request = OrdersByUserIdRequest.newBuilder()
                .setUserId(userId)
                .build();
        try {
            OrdersListResponse response = blockingStub.getOrdersByUserId(request);
            System.out.println("Recibidass " + response.getOrdersCount() + " ordenes para usuario " + userId);
            return response;
        } catch (StatusRuntimeException e) {
            System.err.println("gRPC Error while fetching orders for user " + userId + ": " + e.getStatus());
            // Re-lanzar para que el gateway lo maneje
            throw e;
        } catch (Exception e) {
             System.err.println("Unexpected error while fetching orders for user " + userId + ": " + e.getMessage());
             e.printStackTrace();
              throw new StatusRuntimeException(io.grpc.Status.INTERNAL.withDescription("Error inesperado en cliente gRPC getOrdersByUserId"));
        }
    }

  @PreDestroy
  public void shutdown() {
    if (channel!= null) {
      System.out.println("Shutting down order gRPC channel...");
      try {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("gRPC channel shut down successfully.");
      } catch (InterruptedException e) {
        System.out.println("Error shutting down gRPC channel: " + e.getMessage());
        Thread.currentThread().interrupt(); // Restore the interrupted status
      }
    }
  }
}