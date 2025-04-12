package service

import (
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"path"
	"strconv"

	user "github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/genproto/users"
)

// UserService is the concrete implementation of the user service.
type UserService struct {
	// In-memory list of users loaded from a JSON file.
	users []*user.User
}

// Define the path to the JSON file that holds the user data.
var usersFilePath = path.Join("C:/Users/Victor J. Villadiego/Distributed-Systems-gRPC/microservices/user_service", "users.json")

// loadUsersFromFile reads the JSON file and unmarshals the user data.
func loadUsersFromFile() ([]*user.User, error) {
    data, err := ioutil.ReadFile(usersFilePath)
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
func (s *UserService) GetUser(ctx context.Context, req *user.GetUsersRequest) ([]*user.User, error) {
	fmt.Printf("GetUser called with user_id: %+v\n", req.UserId)
	// Print all loaded user IDs
	fmt.Print("Loaded user IDs: ")
	for _, u := range s.users {
		fmt.Printf("%s ", u.UserId)
	}
	fmt.Println()
	if req.UserId != 0 {
		targetID := strconv.Itoa(int(req.UserId))
		for _, u := range s.users {
			if u.UserId == targetID {
				return []*user.User{u}, nil
			}
		}
		return nil, fmt.Errorf("user with id %s not found", targetID)
	}
	// If no specific user is requested, return all users.
	return s.users, nil
}

// UpdateUserBalance updates the balance of the user specified in the request.
// After updating in memory, it saves the updated list back to the JSON file.
func (s *UserService) UpdateUserBalance(ctx context.Context, req *user.UpdateBalanceRequest) error {
	if req.UserId == 0 {
		return fmt.Errorf("no user id provided")
	}
	targetID := strconv.Itoa(int(req.UserId))
	for _, u := range s.users {
		if u.UserId == targetID {
			// Debug: print the current and new balance values.
			fmt.Printf("Updating balance for user %s: current balance = %.2f, new balance = %.2f\n", targetID, u.Balance, req.NewBalance)
			
			u.Balance = req.NewBalance
			
			// Persist the updated user data to the JSON file.
			if err := saveUsersToFile(s.users); err != nil {
				return fmt.Errorf("failed to save updated balance: %v", err)
			}
			
			// For debugging, you can also read back the file and log its content:
			data, err := json.MarshalIndent(s.users, "", "  ")
			if err == nil {
				fmt.Println("Updated users data saved to file:", string(data))
			}
			
			return nil
		}
	}
	return fmt.Errorf("user with id %s not found", targetID)
}
