package com.eafit.api_gateway.grpc;

/* import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import usuario.UserServiceGrpc;
import usuario.UserRequest;
import usuario.UserResponse; */


//
import org.springframework.stereotype.Service;
@Service
//
public class UserServiceClient {
    /* private final UserServiceGrpc.UserServiceBlockingStub blockingStub;

    public UserServiceClient(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        blockingStub = UserServiceGrpc.newBlockingStub(channel);
    }

    public UserResponse getUser(String userId) {
        UserRequest request = UserRequest.newBuilder().setUserId(userId).build();
        return blockingStub.getUser(request);
    } */

    public String getUser(String userId) {
        // Simula una respuesta del microservicio
        return "{\"id\": \"" + userId + "\", \"name\": \"Usuario Mock\", \"email\": \"mock@example.com\"}";
    }

}