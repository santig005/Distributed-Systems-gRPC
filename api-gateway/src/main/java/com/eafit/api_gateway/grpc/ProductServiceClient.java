  package com.eafit.api_gateway.grpc;

  import io.grpc.ManagedChannel;
  import io.grpc.ManagedChannelBuilder;

  import org.springframework.beans.factory.annotation.Value;
  import org.springframework.stereotype.Service;

  // Importa las clases generadas por el compilador de proto
  import product.ProductServiceGrpc;
  import product.Product.ProductRequest;
  import product.Product.ProductResponse;

  import jakarta.annotation.PostConstruct;

  @Service
  public class ProductServiceClient {

    @Value("${services.product.host}")
      private String productServiceHost;

      @Value("${services.product.port}")
      private int productServicePort;
      
      // Usa el stub generado para el servicio de producto
      private ProductServiceGrpc.ProductServiceBlockingStub blockingStub;

      @PostConstruct
      public void init() {
          ManagedChannel channel = ManagedChannelBuilder.forAddress(productServiceHost, productServicePort)
                  .usePlaintext() // Cambiar a TLS en producción
                  .build();
          blockingStub = ProductServiceGrpc.newBlockingStub(channel);
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
  }
