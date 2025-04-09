package service

import (
	"context"

	"github.com/santig05/Distributed-Systems-gRPC/services/genproto/user"
)

var ordersDb = make([]*user.User, 0)

type UserService struct {
	// store
}

func NewOrderService() *UserService {
	return &UserService{}
}

func (s *UserService) GetOrders(ctx context.Context) []*user.User {
	return ordersDb
}