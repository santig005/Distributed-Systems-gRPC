package handler

import (
	"context"

	"github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/genproto/users"
	"github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/types"
	"google.golang.org/grpc"
)

type UserGrpcHandler struct {
	usersService types.UserService
	user.UnimplementedUserServiceServer
}

func NewGrpcUserService(grpc *grpc.Server, usersService types.UserService) {
	gRPCHandler := &UserGrpcHandler{
		usersService: usersService,
	}

	// register the OrderServiceServer
	user.RegisterUserServiceServer(grpc, gRPCHandler)
}


func (h *UserGrpcHandler) GetOrders(ctx context.Context, req *user.GetUsersRequest) (*user.GetUserResponse, error) {
	o := h.usersService.GetUsers(ctx)
	res := &user.GetUserResponse{
		Users: o,
	}

	return res, nil
}