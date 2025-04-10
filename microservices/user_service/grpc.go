package main

import (
	"log"
	"net"

	handler "github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/handler/user"
	"github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/service"
	"google.golang.org/grpc"
)

type gRPCServer struct {
	addr string
}

func NewGRPCServer(addr string) *gRPCServer {
	return &gRPCServer{addr: addr}
}

func (s *gRPCServer) Run() error {
	lis, err := net.Listen("tcp", s.addr)
	if err != nil {
		log.Fatalf("failed to listen: %v", err)
	}

	grpcServer := grpc.NewServer()

	// register our grpc service
	userService:= service.NewUserService()
	// register the grpc handler with the grpc server
	handler.NewGrpcUserService(grpcServer, userService)

	log.Println("Starting gRPC server on", s.addr)

	return grpcServer.Serve(lis)
}