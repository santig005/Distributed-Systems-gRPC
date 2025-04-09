package types

import (
	"context"

	"github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/genproto/users"
)

type UserService interface {
	GetUsers(context.Context) []*user.User
}