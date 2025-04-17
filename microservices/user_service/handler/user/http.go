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
	router.HandleFunc("/user/balance", h.UpdateBalance)
}

// GetUser es el handler que procesa la petición para obtener un usuario.
func (h *UserHttpHandler) GetUser(w http.ResponseWriter, r *http.Request) {
	// Validamos que el método HTTP sea GET.
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// Parseamos la petición a GetUsersRequest.
	var req user.GetUserRequest
	if err := util.ParseJSON(r, &req); err != nil {
		util.WriteError(w, http.StatusBadRequest, err)
		return
	}

	// Llamamos al servicio para obtener el usuario.
	singleUser, err := h.userService.GetUser(r.Context(), &req)
	if err != nil {
		util.WriteError(w, http.StatusInternalServerError, err)
		return
	}

	// Envolvemos el usuario en un slice para el campo repeated.
	res := &user.GetUserResponse{
		Status: "success",
		Users:  []*user.User{singleUser},
	}
	util.WriteJSON(w, http.StatusOK, res)
}

// UpdateBalance maneja la petición HTTP para actualizar el balance del usuario.
func (h *UserHttpHandler) UpdateBalance(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost && r.Method != http.MethodPut {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    var req user.UpdateBalanceRequest
    if err := util.ParseJSON(r, &req); err != nil {
        util.WriteError(w, http.StatusBadRequest, err)
        return
    }

    // Ahora recibimos *user.UpdateBalanceResponse directamente
    resp, err := h.userService.UpdateUserBalance(r.Context(), &req)
    if err != nil {
        util.WriteError(w, http.StatusInternalServerError, err)
        return
    }

    // Escribimos tal cual la respuesta del servicio
    util.WriteJSON(w, http.StatusOK, resp)
}
