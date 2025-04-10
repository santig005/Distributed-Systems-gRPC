import grpc from '@grpc/grpc-js';
import protoLoader from '@grpc/proto-loader';
import dotenv from 'dotenv';
import path from 'path';
import { fileURLToPath } from 'url';
import { createOrder, getOrder } from './orders.js';

dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// === Configuración ===
const ORDER_PROTO_PATH = path.join(__dirname, process.env.ORDER_PROTO_PATH);
const PRODUCT_PROTO_PATH = path.join(__dirname, process.env.PRODUCT_PROTO_PATH);
const PRODUCT_SERVICE_URL = process.env.PRODUCT_SERVICE_URL;
const HOST = process.env.ORDER_SERVICE_HOST || '0.0.0.0';
const PORT = process.env.ORDER_SERVICE_PORT || '50054';
const ADDRESS = `${HOST}:${PORT}`;

// === Cargar protos ===
const orderDefinition = protoLoader.loadSync(ORDER_PROTO_PATH);
const orderProto = grpc.loadPackageDefinition(orderDefinition).order;

const productDefinition = protoLoader.loadSync(PRODUCT_PROTO_PATH);
const productProto = grpc.loadPackageDefinition(productDefinition).product;

// === Cliente gRPC para product_service ===
const productClient = new productProto.ProductService(
  PRODUCT_SERVICE_URL,
  grpc.credentials.createInsecure()
);

// === Handlers gRPC ===

function createOrderHandler(call, callback) {
  const { user_id, product_ids } = call.request;

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
      const total = productos.reduce((acc, p) => acc + p.price, 0);

      const orderData = { user_id, product_ids, total };
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
    callback(null, order);
  } else {
    callback(new Error('Pedido no encontrado'));
  }
}

// === Iniciar servidor gRPC ===

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
