import grpc from '@grpc/grpc-js';
import protoLoader from '@grpc/proto-loader';
import dotenv from 'dotenv';
import path from 'path';
import { fileURLToPath } from 'url';
import { createOrder, getOrder } from './orders.js';

dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// === Configuration ===
const ORDER_PROTO_PATH = path.join(__dirname, process.env.ORDER_PROTO_PATH);
const PRODUCT_PROTO_PATH = path.join(__dirname, process.env.PRODUCT_PROTO_PATH);
const USER_PROTO_PATH = path.join(__dirname, process.env.USER_PROTO_PATH);

const PRODUCT_SERVICE_URL = process.env.PRODUCT_SERVICE_URL;
const USER_SERVICE_URL = process.env.USER_SERVICE_URL;

const HOST = process.env.ORDER_SERVICE_HOST || '0.0.0.0';
const PORT = process.env.ORDER_SERVICE_PORT || '50054';
const ADDRESS = `${HOST}:${PORT}`;

// === Load proto definitions ===
const orderDefinition = protoLoader.loadSync(ORDER_PROTO_PATH);
const orderProto = grpc.loadPackageDefinition(orderDefinition).order;

const productDefinition = protoLoader.loadSync(PRODUCT_PROTO_PATH);
const productProto = grpc.loadPackageDefinition(productDefinition).product;

const userDefinition = protoLoader.loadSync(USER_PROTO_PATH);
const userProto = grpc.loadPackageDefinition(userDefinition).usuario;

// === Create gRPC clients ===
const productClient = new productProto.ProductService(
  PRODUCT_SERVICE_URL,
  grpc.credentials.createInsecure()
);

const userClient = new userProto.UserService(
  USER_SERVICE_URL,
  grpc.credentials.createInsecure()
);

// === gRPC Handlers ===

function createOrderHandler(call, callback) {
  const { user_id, product_ids } = call.request;
  
  // First, call the UserService to get user information.
  userClient.GetUser({ user_id }, (err, userResponse) => {
    if (err) {
      console.error(`❌ Error al obtener usuario ${user_id}:`, err.message);
      return callback(new Error(`Usuario con ID ${user_id} no disponible`));
    }
    console.log("User fetched:", userResponse);
    
    if (userResponse.status !== "success") {
      return callback(new Error(`User service returned status: ${userResponse.status}`));
    }
    
    // Now, call the ProductService for every product id.
    Promise.all(
      product_ids.map((id) => {
        return new Promise((resolve, reject) => {
          productClient.GetProduct({ id }, (err, product) => {
            if (err) {
              console.error(`❌ Error al obtener producto ${id}:`, err.message);
              return reject(new Error(`Producto con ID ${id} no disponible`));
            }
            resolve(product);
          });
        });
      })
    )
      .then((productos) => {
        // Calculate the total price.
        const total = productos.reduce((acc, p) => acc + p.price, 0);
  
        const orderData = { user_id, product_ids, total };
        const order = createOrder(orderData);
        callback(null, order);
      })
      .catch((err) => {
        callback(err);
      });
  });
}

function getOrderHandler(call, callback) {
  const order = getOrder(call.request.id);
  if (order) {
    callback(null, order);
  } else {
    callback(new Error('Pedido no encontrado'));
  }
}

// === Start gRPC Server ===

function main() {
  const server = new grpc.Server();
  server.addService(orderProto.OrderService.service, {
    CreateOrder: createOrderHandler,
    GetOrder: getOrderHandler,
  });

  server.bindAsync(ADDRESS, grpc.ServerCredentials.createInsecure(), () => {
    console.log(`🟢 OrderService escuchando en ${ADDRESS}`);
    server.start();

    // === First, test the user client call ===
    userClient.GetUser({ userId: 1 }, (err, userResp) => {
      if (err) {
        console.error(`❌ Error al obtener usuario:`, err.message);
      } else {
        console.log("User fetched (test):", userResp["users"]);

        // Only if the user is successfully fetched, call product service
        productClient.GetProduct({ id: "1" }, (err, product) => {
          if (err) {
            console.error(`❌ Error al obtener producto (test):`, err.message);
          } else {
            console.log("Product fetched (test):", product);
            // Simulate logic: check if user balance covers product price and then create order.
            if (userResp["users"][0]["balance"] - product.price > 0) {
              const orderData = {
                user_id: userResp["users"][0]["userId"],
                product_ids: ["1"], // Example: ordering product with ID "2"
                total: product.price
              };
              const order = createOrder(orderData);
              console.log("Order created (test):", order);
              const newBalance = userResp["users"][0]["balance"] - product.price;
              // Test updating the user balance
              userClient.UpdateUserBalance({ userId: 1, newBalance: newBalance }, (err, updateResp) => {
                if (err) {
                  console.error(`❌ Error updating user balance (test):`, err.message);
                } else {
                  console.log("User balance updated (test):", updateResp);
                }
              });
            } else {
              console.error("Insufficient balance for product purchase (test).");
            }
          }
        });
      }
    });
  });
}

main();