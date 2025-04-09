package handler

import (
	"net/http"

	user "github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/genproto/users"
	"github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/utils"
	"github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/types"
)

// UserHttpHandler es el manejador HTTP para el servicio de usuario.
type UserHttpHandler struct {
	userService types.UserService
}

// NewHttpUserHandler retorna una nueva instancia de UserHttpHandler.
func NewHttpUserHandler(userService types.UserService) *UserHttpHandler {
	return &UserHttpHandler{
		userService: userService,
	}
}

// RegisterRouter registra las rutas del servicio de usuario en el multiplexer HTTP.
func (h *UserHttpHandler) RegisterRouter(router *http.ServeMux) {
	router.HandleFunc("/user", h.GetUser)
}

// GetUser es el handler que procesa la petición para obtener un usuario.
func (h *UserHttpHandler) GetUser(w http.ResponseWriter, r *http.Request) {
	// Validamos que el método HTTP sea GET.
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// Intentamos parsear la petición JSON a la estructura GetUsersRequest.
	var req user.GetUsersRequest
	err := util.ParseJSON(r, &req)
	if err != nil {
		util.WriteError(w, http.StatusBadRequest, err)
		return
	}

	// Llamamos al servicio para obtener los usuarios usando el request parseado.
	usersList, err := h.userService.GetUser(r.Context(), &req)
	if err != nil {
		util.WriteError(w, http.StatusInternalServerError, err)
		return
	}

	// Se arma la respuesta y se envía como JSON.
	res := &user.GetUserResponse{
		Status: "success",
		Users:  usersList,
	}
	util.WriteJSON(w, http.StatusOK, res)
}
