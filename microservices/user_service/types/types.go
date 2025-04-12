package types

import (
	"context"

	user "github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/genproto/users"
)

// UserService define el contrato para el servicio de usuario.
type UserService interface {
	// GetUser recibe un contexto y un GetUsersRequest, y retorna una lista de usuarios y un error (si ocurre).
	GetUser(ctx context.Context, req *user.GetUsersRequest) ([]*user.User, error)
	UpdateUserBalance(ctx context.Context, req *user.UpdateBalanceRequest) error
}