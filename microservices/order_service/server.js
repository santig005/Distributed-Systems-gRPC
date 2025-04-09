import grpc from '@grpc/grpc-js';
import protoLoader from '@grpc/proto-loader';
import dotenv from 'dotenv';
import path from 'path';
import { fileURLToPath } from 'url';
import { createOrder, getOrder } from './orders.js';

dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Configuración
const PROTO_PATH = path.join(__dirname, process.env.PROTO_PATH);
const HOST = process.env.ORDER_SERVICE_HOST || '0.0.0.0';
const PORT = process.env.ORDER_SERVICE_PORT || '50054';
const ADDRESS = `${HOST}:${PORT}`;

// Cargar proto
const packageDefinition = protoLoader.loadSync(PROTO_PATH);
const orderProto = grpc.loadPackageDefinition(packageDefinition).order;

// Servicio gRPC
function createOrderHandler(call, callback) {
  const order = createOrder(call.request);
  callback(null, order);
}

function getOrderHandler(call, callback) {
  const order = getOrder(call.request.id);
  if (order) {
    callback(null, order);
  } else {
    callback(new Error('Pedido no encontrado'));
  }
}

// Iniciar servidor
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
