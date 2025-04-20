import grpc from '@grpc/grpc-js';
import protoLoader from '@grpc/proto-loader';
import dotenv from 'dotenv';
import path from 'path';
import { fileURLToPath } from 'url';
import { createOrder, getOrder } from './orders.js';
import logger from './logger.js';

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

function createOrderHandler(call, callback) {
  logger.info('Lests create an order');
  const { userId, productIds } = call.request;
  logger.info(`user: ${userId}`);
  logger.info(`product_ids: ${productIds}`);

  Promise.all(
    productIds.map((id) => {
      logger.info(`Fetching product with ID: ${id}`);
      return new Promise((resolve, reject) => {
        productClient.GetProduct({ id }, (err, product) => {
          if (err) {
            logger.error(`❌ Error fetching product ${id}: ${err.message}`);
            return reject(new Error(`Product with ID ${id} is not available`));
          }
          logger.info(
            `Product fetched successfully: ${JSON.stringify(product)}`
          );
          resolve(product);
        });
      });
    })
  )
    .then((productos) => {
      const total = productos.reduce((acc, p) => acc + p.price, 0);

      const orderData = { userId, productIds };
      const order = createOrder(orderData);
      callback(null, order);
    })
    .catch((err) => {
      callback(err);
    });
}

function getOrderHandler(call, callback) {
  const order = getOrder(call.request.id);
  if (order) {
    logger.info(`Sending gRPC response: ${JSON.stringify(order)}`);
    callback(null, order);
  } else {
    logger.error('Order not found');
    callback(new Error('Order not found'));
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
    logger.info(`🟢 OrderService listening on ${ADDRESS}`);
    server.start();

    // === First, test the user client call ===
    userClient.GetUser({ userId: 1 }, (err, userResp) => {
      if (err) {
        logger.error(`Error fetching user: ${err.message}`);
      } else {
        logger.info(`User fetched (test): ${userResp.users}`);

        // Only if the user is successfully fetched, call product service
        productClient.GetProduct({ id: '1' }, (err, product) => {
          if (err) {
            logger.error(`Error fetching product (test): ${err.message}`);
          } else {
            logger.info(`Product fetched (test): ${product}`);
            // Simulate logic: check if user balance covers product price and then create order.
            const user = userResp.users[0];
            if (user.balance - product.price > 0) {
              const orderData = {
                user_id: user.userId,
                product_ids: ['1'], // Example: ordering product with ID "1"
                total: product.price,
              };
              const order = createOrder(orderData);
              logger.info(
                `Order created (test): ${JSON.stringify(order, null, 2)}`
              );

              const newBalance = user.balance - product.price;
              // Test updating the user balance
              userClient.UpdateUserBalance(
                { userId: 1, newBalance: newBalance },
                (err, updateResp) => {
                  if (err) {
                    logger.error(
                      `Error updating user balance (test):
                      ${err.message}`
                    );
                  } else {
                    // <-- New: print the full JSON response of the updated user
                    logger.info(
                      'Updated user info (test):\n' +
                        JSON.stringify(updateResp, null, 2)
                    );
                  }
                }
              );
            } else {
              logger.error('Insufficient balance for product purchase (test).');
            }
          }
        });
      }
    });
  });
}

main();
