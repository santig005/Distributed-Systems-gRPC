package main

import (
	"context"
	"fmt"
	"log"
	"os"
	"os/signal"
	"syscall"
	"time"

	amqp "github.com/rabbitmq/amqp091-go"
	"github.com/joho/godotenv"

	server "github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/server"
	user "github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/genproto/users"
	grpcserver "github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/grpc"
	service "github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/service"
)

const (
	defaultHTTPPort    = "8000"
	defaultGRPCPort    = "9000"
	defaultRabbitMQURL = "amqp://guest:guest@localhost:5672/"
	userQueueName      = "user-service-queue"
)

// failOnError panics si ocurre un error crítico
func failOnError(err error, msg string) {
	if err != nil {
		log.Panicf("%s: %s", msg, err)
	}
}

// startRabbitMQConsumer declara y consume mensajes de la cola.
// Por cada mensaje (cuerpo = user_id) llama a GetUser de userService.
func startRabbitMQConsumer(userService *service.UserService) {
	// Carga la URL o usa la por defecto
	rabbitURL := os.Getenv("RABBITMQ_URL")
	if rabbitURL == "" {
		rabbitURL = defaultRabbitMQURL
	}

	// Reintenta la conexión
	var conn *amqp.Connection
	var err error
	for i := 0; i < 5; i++ {
		conn, err = amqp.Dial(rabbitURL)
		if err == nil {
			break
		}
		log.Printf("RabbitMQ connect error: %v, retrying in 5s...", err)
		time.Sleep(5 * time.Second)
	}
	failOnError(err, "cannot connect to RabbitMQ")
	defer conn.Close()
	log.Println("✅ Connected to RabbitMQ")

	ch, err := conn.Channel()
	failOnError(err, "failed to open channel")
	defer ch.Close()

	q, err := ch.QueueDeclare(
		userQueueName,
		true,  // durable
		false, // delete when unused
		false, // exclusive
		false, // no-wait
		nil,
	)
	failOnError(err, "failed to declare queue")

	msgs, err := ch.Consume(
		q.Name,
		"",    // consumer
		false, // auto-ack = false
		false,
		false,
		false,
		nil,
	)
	failOnError(err, "failed to register consumer")

	log.Printf("🕒 Waiting for messages on %q...", q.Name)
	for d := range msgs {
		body := string(d.Body)
		log.Printf("➡️  Got message: %s", body)

		// Parsear user_id
		var userID int32
		_, err := fmt.Sscanf(body, "/api/users/%d", &userID)
		if err != nil {
			log.Printf("❌ ID inválido '%s': %v", body, err)
			d.Ack(false)
			continue
		}

		// Llamar a GetUser directamente
		_, err = userService.GetUser(context.Background(), &user.GetUserRequest{
			UserId: userID,
		})
		if err != nil {
			log.Printf("⚠️  GetUser(%d) error: %v", userID, err)
			// Sin Ack para reintentar
			continue
		}

		log.Printf("✅ GetUser(%d) succeeded", userID)
		d.Ack(false)
	}
}

func main() {
	// Cargar .env si existe
	_ = godotenv.Load()

	// 1) Inicializar la lógica de negocio
	userService := service.NewUserService()

	// 2) Servidor HTTP
	httpAddr := ":" + func() string {
		if p := os.Getenv("USER_SERVICE_HTTPPORT"); p != "" {
			return p
		}
		return defaultHTTPPort
	}()
	httpServer := server.NewHttpServer(httpAddr)
	go func() {
		log.Printf("🟢 HTTP listening on %s", httpAddr)
		if err := httpServer.Run(); err != nil {
			log.Fatalf("HTTP server error: %v", err)
		}
	}()

	// 3) Servidor gRPC
	grpcAddr := ":" + func() string {
		if p := os.Getenv("USER_SERVICE_GRPCPORT"); p != "" {
			return p
		}
		return defaultGRPCPort
	}()
	go func() {
		log.Printf("🟢 gRPC listening on %s", grpcAddr)
		grpcSrv := grpcserver.NewGRPCServer(grpcAddr)
		if err := grpcSrv.Run(); err != nil {
			log.Fatalf("gRPC server error: %v", err)
		}
	}()

	// 4) Consumidor RabbitMQ
	go startRabbitMQConsumer(userService)

	// 5) Graceful shutdown
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit
	log.Println("🔴 Shutting down...")

	// Aquí podrías cerrar recursos adicionales si los expones
}