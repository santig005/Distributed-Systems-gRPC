package service

import (
	"context"

	user "github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/genproto/users"
)

// UserService es la implementación concreta de la interfaz types.UserService.
type UserService struct {
	// Aquí puedes agregar dependencias, como conexión a base de datos, etc.
}

// NewUserService retorna una nueva instancia de UserService.
func NewUserService() *UserService {
	return &UserService{}
}

// GetUser implementa el método definido en types.UserService.
// En este ejemplo se retorna una lista de usuarios dummy.
func (s *UserService) GetUser(ctx context.Context, req *user.GetUsersRequest) ([]*user.User, error) {
	// Aquí deberías implementar la lógica real para obtener los usuarios
	// A modo de ejemplo, retorna un usuario dummy con UserId "1"
	dummyUser := &user.User{
		UserId: "1",
	}
	return []*user.User{dummyUser}, nil
}
