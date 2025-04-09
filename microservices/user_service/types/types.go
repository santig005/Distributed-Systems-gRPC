package types

import (
	"context"

	"github.com/santig005/Distributed-Systems-gRPC/services/genproto/user"
)

type userService interface {
	CreateOrder(context.Context, *user.User) error
	GetOrders(context.Context) []*user.User
}