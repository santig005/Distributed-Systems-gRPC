package handler

import (
	"net/http"

	"github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/genproto/users"
	"github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/util"
	"github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/types"
)

type UserHttpHandler struct {
	userService types.UserService
}

func NewHttpUserHandler(userService types.UserService) *UserHttpHandler {
	handler := &UserHttpHandler{
		userService: userService,
	}

	return handler
}

func (h *UserHttpHandler) RegisterRouter(router *http.ServeMux) {
	router.HandleFunc("GET /user", h.GetUser)
}

func (h *UserHttpHandler) GetUser(w http.ResponseWriter, r *http.Request) {
	var req user.GetUsersRequest
	err := util.ParseJSON(r, &req)
	if err != nil {
		util.WriteError(w, http.StatusBadRequest, err)
		return
	}

	user := &user.User{
		user_id: 1,
	}

	err = h.userService.GetUser(r.Context(), user1)
	if err != nil {
		util.WriteError(w, http.StatusInternalServerError, err)
		return
	}

	res := &user.GetUserResponse{Status: "success"}
	util.WriteJSON(w, http.StatusOK, res)
}