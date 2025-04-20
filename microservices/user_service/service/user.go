package service

import (
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"os"
	"strconv"

	user "github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/genproto/users"
)

// UserService is the concrete implementation of the user service.
type UserService struct {
	// In-memory list of users loaded from a JSON file.
	users []*user.User
}

// Define the path to the JSON file that holds the user data.
var usersFilePath = "users.json"

// loadUsersFromFile reads the JSON file and unmarshals the user data.
func loadUsersFromFile() ([]*user.User, error) {
	wd, _ := os.Getwd()
	fmt.Println("Directorio actual:", wd)
	data, err := os.ReadFile(usersFilePath)
	if err != nil {
		return nil, err
	}
	var loadedUsers []*user.User
	err = json.Unmarshal(data, &loadedUsers)
	if err != nil {
		return nil, err
	}
	return loadedUsers, nil
}

// saveUsersToFile marshals the users slice and writes it back to the JSON file.
func saveUsersToFile(users []*user.User) error {
	data, err := json.MarshalIndent(users, "", "  ")
	if err != nil {
		return err
	}
	return ioutil.WriteFile(usersFilePath, data, 0644)
}

// NewUserService creates a new instance of UserService, attempting to load
// users from the JSON file. If the file does not exist, it uses default data.
func NewUserService() *UserService {
	loadedUsers, err := loadUsersFromFile()
	if err != nil {
		fmt.Println("Failed to load users from file, using default values. Error:", err)
		loadedUsers = []*user.User{
			{
				UserId:  "1",
				Name:    "Alice",
				Email:   "alice@example.com",
				Balance: 100.50,
			},
			{
				UserId:  "2",
				Name:    "Bob",
				Email:   "bob@example.com",
				Balance: 200.00,
			},
			{
				UserId:  "3",
				Name:    "Charlie",
				Email:   "charlie@example.com",
				Balance: 300,
			},
			{
				UserId:  "4",
				Name:    "Diana",
				Email:   "diana@example.com",
				Balance: 150.25,
			},
		}
		// Save the default users so that next time the file exists.
		if err := saveUsersToFile(loadedUsers); err != nil {
			fmt.Println("Failed to save default users to file:", err)
		}
	}

	return &UserService{
		users: loadedUsers,
	}
}

// GetUser implements the user lookup logic for the service.
// If req.UserId is non-zero, it searches for a matching user and returns it in a slice.
// nueva firma que devuelve un solo usuario
func (s *UserService) GetUser(ctx context.Context, req *user.GetUserRequest) (*user.User, error) {
	fmt.Printf("GetUser called with user_id: %v\n", req.UserId)
	if req.UserId == 0 {
		return nil, fmt.Errorf("no user id provided")
	}
	targetID := strconv.Itoa(int(req.UserId))
	for _, u := range s.users {
		if u.UserId == targetID {
			return u, nil
		}
	}
	return nil, fmt.Errorf("user with id %s not found", targetID)
}

// UpdateUserBalance updates the balance of the user specified in the request.
// After updating in memory, it saves the updated list back to the JSON file.
// import user "github.com/santig005/Distributed-Systems-gRPC/libs/protobufs"

func (s *UserService) UpdateUserBalance(
	ctx context.Context,
	req *user.UpdateBalanceRequest,
) (*user.UpdateBalanceResponse, error) {
	if req.UserId == 0 {
		return nil, fmt.Errorf("no user id provided")
	}
	targetID := strconv.Itoa(int(req.UserId))
	fmt.Printf("UpdateUserBalance called with user_id: %v\n", targetID)
	for _, u := range s.users {
		if u.UserId == targetID {
			// Aquí u es *user.User, así que u.Balance sí existe.
			fmt.Printf(
				"Updating balance for user %s: current = %.2f, new = %.2f\n",
				targetID, u.Balance, req.NewBalance,
			)
			u.Balance = req.NewBalance

			if err := saveUsersToFile(s.users); err != nil {
				return nil, fmt.Errorf("failed to save updated balance: %v", err)
			}

			// Devolvemos la respuesta completa incluyendo el usuario actualizado
			return &user.UpdateBalanceResponse{
				Status: "balance updated",
				User:   u,
			}, nil
		}
	}

	return nil, fmt.Errorf("user with id %s not found", targetID)
}
