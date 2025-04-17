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


func (h *UserGrpcHandler) GetUser(ctx context.Context, req *user.GetUsersRequest) (*user.GetUserResponse, error) {
	// Llamamos al método de la interfaz, pasándole el contexto y el request.
	usersList, err := h.usersService.GetUser(ctx, req)
	if err != nil {
		return nil, err
	}

	// Se arma la respuesta según el proto.
	res := &user.GetUserResponse{
		Status: "success",
		Users:  usersList,
	}
	return res, nil
}

// UpdateUserBalance handles the UpdateUserBalance RPC call.
func (h *UserGrpcHandler) UpdateUserBalance(ctx context.Context, req *user.UpdateBalanceRequest) (*user.UpdateBalanceResponse, error) {
	// Delegate the balance update to the underlying service.
	err := h.usersService.UpdateUserBalance(ctx, req)
	if err != nil {
		return nil, err
	}
	return &user.UpdateBalanceResponse{
		Status: "balance updated",
	}, nil
}