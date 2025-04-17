package com.eafit.api_gateway.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import order.OrderServiceGrpc;
import order.Order.OrderRequest;
import order.Order.OrderResponse;
import order.Order.OrderByIdRequest;
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

  public OrderResponse createOrder(String userId, List<String> productIds, float total) {
    OrderRequest request = OrderRequest.newBuilder()
        .setUserId(userId)
        .addAllProductIds(productIds)
        .setTotal(total)
        .build();
    return blockingStub.createOrder(request);
  }

  public OrderResponse getOrder(String orderId) {
    try {
      System.out.println("Order ID: " + orderId);
      OrderByIdRequest request = OrderByIdRequest.newBuilder()
        .setId(orderId)
        .build();
        System.out.println("Request: " + request.toString());
      OrderResponse o= blockingStub.getOrder(request);
      System.out.println("Response: " + o.toString());
      return o;
    } catch (Exception e) {
      System.err.println("Error while fetching order: " + e.getMessage());
      e.printStackTrace();
      return null; // Handle this appropriately in your application
    }
  }

  /* @PreDestroy
  public void shutdown() {
    if (channel!= null) {
      System.out.println("Shutting down gRPC channel...");
      try {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("gRPC channel shut down successfully.");
      } catch (InterruptedException e) {
        System.out.println("Error shutting down gRPC channel: " + e.getMessage());
        Thread.currentThread().interrupt(); // Restore the interrupted status
      }
    }
  } */
}