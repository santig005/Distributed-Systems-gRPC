package service

import (
	"context"
	"fmt"
	"time"

	"google.golang.org/grpc"

	// Adjust the import path to match your go.mod and folder structure.
	product "github.com/santig005/Distributed-Systems-gRPC/microservices/user_service/genproto/products"
)

// ProductClient wraps the gRPC client to call the Node.js product service.
type ProductClient struct {
	client product.ProductServiceClient
	conn   *grpc.ClientConn
}

// NewProductClient dials the Node.js product service at the specified address.
func NewProductClient(address string) (*ProductClient, error) {
	// In production, you'd likely configure secure credentials instead of grpc.WithInsecure().
	conn, err := grpc.Dial(address, grpc.WithInsecure(), grpc.WithBlock(), grpc.WithTimeout(5*time.Second))
	if err != nil {
		return nil, fmt.Errorf("failed to dial product service: %w", err)
	}
	c := product.NewProductServiceClient(conn)
	return &ProductClient{client: c, conn: conn}, nil
}

// Close closes the underlying gRPC connection.
func (p *ProductClient) Close() error {
	return p.conn.Close()
}

// GetProduct fetches a product by ID from the Node.js product service.
// Note the return type has been changed to *product.ProductResponse.
func (p *ProductClient) GetProduct(ctx context.Context, productID string) (*product.ProductResponse, error) {
	req := &product.ProductRequest{
		Id: productID,
	}

	resp, err := p.client.GetProduct(ctx, req)
	if err != nil {
		return nil, fmt.Errorf("error calling GetProduct: %w", err)
	}
	return resp, nil
}