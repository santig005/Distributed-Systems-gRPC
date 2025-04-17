package main

import (
    "log"

    server "github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/server"
	grpcserver "github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/grpc"

    // Also import your gRPC package if needed:
    // "github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/grpcserver"
)

func main() {
    httpServer := server.NewHttpServer(":8000")
    go func() {
        if err := httpServer.Run(); err != nil {
            log.Fatalf("HTTP server error: %v", err)
        }
    }()

    // Assuming you have defined NewGRPCServer similarly in a separate package or file:
    grpcServer := grpcserver.NewGRPCServer(":9000") // adjust import if necessary
    if err := grpcServer.Run(); err != nil {
        log.Fatalf("gRPC server error: %v", err)
    }
}