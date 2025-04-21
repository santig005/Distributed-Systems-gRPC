  package com.eafit.api_gateway.grpc;

  import io.grpc.ManagedChannel;
  import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
  import org.springframework.stereotype.Service;

  // Importa las clases generadas por el compilador de proto
  import product.ProductServiceGrpc;
  import product.Product.ProductRequest;
  import product.Product.ProductResponse;

  import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import order.OrderServiceGrpc;

  @Service
  public class ProductServiceClient {

    @Value("${services.product.host}")
      private String productServiceHost;

      @Value("${services.product.port}")
      private int productServicePort;
      
      // Usa el stub generado para el servicio de producto
      private ProductServiceGrpc.ProductServiceBlockingStub blockingStub;

      private ManagedChannel channel;

      @PostConstruct
      public void init() {
        this.channel = ManagedChannelBuilder.forAddress(productServiceHost, productServicePort)
            .usePlaintext() // Use TLS in production
            .build();
        blockingStub = ProductServiceGrpc.newBlockingStub(this.channel);
      }

      // Si el método en el proto es getProduct, usa ese nombre
      public ProductResponse getProduct(String productId) {
          // Crea la solicitud usando el builder del proto
          ProductRequest request = ProductRequest.newBuilder()
                  .setId(productId)
                  .build();
          // Realiza la llamada remota con el stub
          return blockingStub.getProduct(request);
      }
      @PreDestroy
      public void shutdown() {
        if (channel!= null) {
          System.out.println("Shutting down product gRPC channel...");
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
