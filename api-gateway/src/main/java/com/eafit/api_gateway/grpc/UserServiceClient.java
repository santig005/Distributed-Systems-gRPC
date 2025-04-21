package com.eafit.api_gateway.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// Importa las clases generadas por el compilador de proto
import usuario.UserServiceGrpc;
import usuario.Users.GetUserResponse;


import usuario.Users.GetUserRequest; // Ensure this matches the actual package and class name


import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import product.ProductServiceGrpc;


//import jakarta.annotation.PostConstruct;

@Service
public class UserServiceClient {

    @Value("${services.user.host}")
    private String userServiceHost;

    @Value("${services.user.port}")
    private int userServicePort;
    
    // Usa el stub generado para el servicio de usuario
    private UserServiceGrpc.UserServiceBlockingStub blockingStub;

    private ManagedChannel channel;

      @PostConstruct
      public void init() {
        this.channel = ManagedChannelBuilder.forAddress(userServiceHost, userServicePort)
            .usePlaintext() // Use TLS in production
            .build();
        blockingStub = UserServiceGrpc.newBlockingStub(this.channel);
      }

    // Si el método en el proto es getUser, usa ese nombre
    public GetUserResponse getUser(String userId) {
        // Crea la solicitud usando el builder del proto
        GetUserRequest request = GetUserRequest.newBuilder()
                .setUserId(Integer.parseInt(userId)) // Asegúrate de que el tipo coincida con lo que espera el servicio
                .build();
        // Realiza la llamada remota con el stub
        return blockingStub.getUser(request);
    }
    @PreDestroy
      public void shutdown() {
        if (channel!= null) {
          System.out.println("Shutting down user gRPC channel...");
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


