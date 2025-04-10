package service

import (
	"context"

	"github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/genproto/users"
)

var ordersDb = make([]*user.UserRequest, 0)

type UserService struct {
	// store
}

func NewUserService() *UserService {
	return &UserService{}
}

func (s *UserService) GetOrders(ctx context.Context) []*user.UserRequest{
	return ordersDb
}