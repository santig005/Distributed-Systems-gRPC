package main

import (
    "github.com/santig005/Distributed-Systems-gRPC/microservices/user_service"
)

func main() {
	httpServer := NewHttpServer(":8000")
	go httpServer.Run()
		
	grpcServer := NewGRPCServer(":9000")
	grpcServer.Run()
}