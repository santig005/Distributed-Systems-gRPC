package types

import (
	"context"

	user "github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/genproto/users"
)

type UserService interface {
	GetUser(ctx context.Context, req *user.GetUserRequest) (*user.User, error)
	// Ahora devuelve el usuario actualizado.
    UpdateUserBalance(ctx context.Context, req *user.UpdateBalanceRequest) (*user.UpdateBalanceResponse, error)
}
