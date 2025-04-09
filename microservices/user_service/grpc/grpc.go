package grpcserver

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

	// Crear la instancia del servicio de usuario
	userService := service.NewUserService()
	// Registrar el handler gRPC con el servidor gRPC
	handler.NewGrpcUserService(grpcServer, userService)

	log.Println("Starting gRPC server on", s.addr)
	return grpcServer.Serve(lis)
}