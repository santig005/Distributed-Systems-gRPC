package server

import (
    "log"
    "net/http"

    handler "github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/handler/user"
    "github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/service"
)

type HttpServer struct {
    Addr string
}

func NewHttpServer(addr string) *HttpServer {
    return &HttpServer{Addr: addr}
}

func (s *HttpServer) Run() error {
    router := http.NewServeMux()

    userService := service.NewUserService()
    userHandler := handler.NewHttpUserHandler(userService)
    userHandler.RegisterRouter(router)

    log.Println("Starting server on", s.Addr)
    return http.ListenAndServe(s.Addr, router)
}