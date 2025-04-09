package handler

import (
	"net/http"

	"github.com/santig005/Distributed-Systems-gRPC/services/genproto/user"
	"github.com/santig005/Distributed-Systems-gRPC/common/util"
	"github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/types"
)

type UserHttpHandler struct {
	userService types.UserService
}

func NewHttpUserHandler(userService types.userService) *UserHttpHandler {
	handler := &UserHttpHandler{
		userService: userService,
	}

	return handler
}

func (h *UserHttpHandler) RegisterRouter(router *http.ServeMux) {
	router.HandleFunc("GET /user", h.GetUser)
}

func (h *UserHttpHandler) GetUser(w http.ResponseWriter, r *http.Request) {
	var req user.GetUserRequest
	err := util.ParseJSON(r, &req)
	if err != nil {
		util.WriteError(w, http.StatusBadRequest, err)
		return
	}

	user1 := &user.User{
		UserID:    42,
	}

	err = h.userService.GetUser(r.Context(), user1)
	if err != nil {
		util.WriteError(w, http.StatusInternalServerError, err)
		return
	}

	res := &user.GetUserResponse{Status: "success"}
	util.WriteJSON(w, http.StatusOK, res)
}