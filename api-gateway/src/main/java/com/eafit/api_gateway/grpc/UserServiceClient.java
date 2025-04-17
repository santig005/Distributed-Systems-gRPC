package com.eafit.api_gateway.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// Importa las clases generadas por el compilador de proto
import usuario.UserServiceGrpc;
import usuario.Users.GetUserResponse;





import usuario.Users.GetUserRequest; // Ensure this matches the actual package and class name


import jakarta.annotation.PostConstruct;


//import jakarta.annotation.PostConstruct;

@Service
public class UserServiceClient {

    @Value("${services.user.host}")
    private String userServiceHost;

    @Value("${services.user.port}")
    private int userServicePort;
    
    // Usa el stub generado para el servicio de usuario
    private UserServiceGrpc.UserServiceBlockingStub blockingStub;

    @PostConstruct
    public void init() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(userServiceHost, userServicePort)
                .usePlaintext() // Cambiar a TLS en producción
                .build();
        blockingStub = UserServiceGrpc.newBlockingStub(channel);
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
}


