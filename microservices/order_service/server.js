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

function getUserHandler(userId) {
  return new Promise((resolve, reject) => {
    userClient.GetUser({ userId }, (err, response) => {
      if (err) {
        console.error(err);
        return reject(err);
      }
      console.log('✔️ User fetched:', response);
      resolve(response);
    });
  });
}
  
function updateUserBalanceHandler(userId, newBalance) {
  return new Promise((resolve, reject) => {
    userClient.UpdateUserBalance({ userId, newBalance }, (err, response) => {
      if (err) {
        console.error(err);
        return reject(err);
      }
      console.log('✔️ User balance updated:', response);
      resolve(response);
    });
  });
}


function createOrderHandler(call, callback) {
  console.log('vamos a crear una order');
  const { userId, productIds } = call.request;
  console.log('user ');
  console.log(userId);
  console.log('product_ids');
  console.log(productIds);

  let userIdInt;
  try {
      // El  user_service (Go) espera un int32 para user_id
      userIdInt = parseInt(userId, 10);
      if (isNaN(userIdInt)) {
          throw new Error("Invalid User ID format, must be a number.");
      }
  } catch (err) {
      console.error(`❌ Invalid User ID format: ${userIdString}`, err);
      return callback({
          code: grpc.status.INVALID_ARGUMENT,
          details: "Invalid User ID format, must be a number.",
      });
  }
  console.log(`Processing order for User ID (int): ${userIdInt}, Product IDs: ${productIds.join(', ')}`);

  Promise.all(
    productIds.map((id) => {
      console.log('vamos a retornar esto');
      console.log(id);
      return new Promise((resolve, reject) => {
        productClient.GetProduct({ id }, (err, product) => {
          if (err) {
            console.error(`❌ Error al obtener producto ${id}:`, err.message);
            return reject(new Error(`Producto con ID ${id} no disponible`));
          }
          console.log('lets resolve');
          resolve(product);
          console.log('resolved');
        });
      });
    })
  )
    .then((productos) => {
      console.log('entramos a then');
      const total = productos.reduce((acc, p) => acc + p.price, 0);
      // now we get the user info and update the balance
     getUserHandler(userIdInt).then((userResp) => {
        const user = userResp.users[0];
        console.log('userResp');
        console.log(userResp);
        console.log('user');  
        console.log(userResp.users[0]);
        if (!user) {
          throw new Error(`Usuario con ID ${userIdInt} no encontrado`);
        }
        console.log('user balance');
        console.log(user.balance);
        console.log('total');
        console.log(total);
        if (user.balance < total) {
          throw new Error(
            `Saldo insuficiente para el pedido. Saldo actual: ${user.balance}, Total del pedido: ${total}`
          );
        }
        console.log('vamos a actualizar el balance');
        updateUserBalanceHandler(
          userIdInt,
          user.balance - total
        ).then(() => {
          console.log('balance actualizado');
           const orderData = { userId, productIds,total };
          const order = createOrder(orderData);
          callback(null, order);
        });
     
    })
    .catch((err) => {
      callback(err);
    });
});
  console.log('salimos de createOrderHandler');
}

function getOrderHandler(call, callback) {
  const order = getOrder(call.request.id);
  if (order) {
    console.log('✔️ Enviando respuesta gRPC:', JSON.stringify(order));
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
  });
}

main();
