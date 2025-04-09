package handler

import (
	"context"

	"github.com/santig005/Distributed-Systems-gRPC/services/genproto/user"
	"github.com/santig005/Distributed-Systems-gRPC/user_service/types"
	"google.golang.org/grpc"
)

type UserGrpcHandler struct {
	userService types.UserService
	user.UnimplementedOrderServiceServer
}

func NewGrpcUserService(grpc *grpc.Server, userService types.userService) {
	gRPCHandler := &UserGrpcHandler{
		userService: userService,
	}

	// register the OrderServiceServer
	user.RegisterOrderServiceServer(grpc, gRPCHandler)
}


func (h *UserGrpcHandler) GetUser(ctx context.Context, req *user.CreateOrderRequest) (*user.GetUserResponse, error) {
	order := &user.User{
		user_id: 1,
	}

	err := h.userService.GetUser(ctx, order)
	if err != nil {
		return nil, err
	}

	res := &user.GetUserResponse{
		Status: "success",
	}

	return res, nil
}