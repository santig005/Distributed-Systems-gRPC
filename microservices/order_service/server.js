import grpc from '@grpc/grpc-js';
import protoLoader from '@grpc/proto-loader';
import dotenv from 'dotenv';
import path from 'path';
import { fileURLToPath } from 'url';
import { createOrder, getOrder } from './orders.js';
import amqp from 'amqplib';

dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname  = path.dirname(__filename);

// === Configuration ===
const ORDER_PROTO_PATH   = path.join(__dirname, process.env.ORDER_PROTO_PATH   || '../../libs/protobufs/order.proto');
const PRODUCT_PROTO_PATH = path.join(__dirname, process.env.PRODUCT_PROTO_PATH || '../../libs/protobufs/product.proto');
const USER_PROTO_PATH    = path.join(__dirname, process.env.USER_PROTO_PATH    || '../../libs/protobufs/users.proto');

const PRODUCT_SERVICE_URL = process.env.PRODUCT_SERVICE_URL || 'product-service:50053';
const USER_SERVICE_URL    = process.env.USER_SERVICE_URL    || 'user-service:50051';

const HOST    = process.env.ORDER_SERVICE_HOST || '0.0.0.0';
const PORT    = process.env.ORDER_SERVICE_PORT || '50054';
const ADDRESS = `${HOST}:${PORT}`;

// --- RabbitMQ Configuration ---
const RABBITMQ_URL = process.env.RABBITMQ_URL || 'amqp://guest:guest@localhost:5672';
const ORDER_QUEUE   = 'order-service-queue';
// --- End RabbitMQ Configuration ---

// === Load proto definitions ===
const orderDef   = protoLoader.loadSync(ORDER_PROTO_PATH);
const orderProto = grpc.loadPackageDefinition(orderDef).order;

const productDef   = protoLoader.loadSync(PRODUCT_PROTO_PATH);
const productProto = grpc.loadPackageDefinition(productDef).product;

const userDef   = protoLoader.loadSync(USER_PROTO_PATH, {
  longs:        String,
  enums:        String,
  defaults:     true,
  oneofs:       true,
});
const userProto = grpc.loadPackageDefinition(userDef).usuario;

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

async function createOrderHandler(call, callback) {
  const { userId, productIds } = call.request;

  if (!userId || !Array.isArray(productIds) || productIds.length === 0) {
    return callback({
      code:    grpc.status.INVALID_ARGUMENT,
      message: "Se requiere userId y al menos un productId.",
    });
  }

  try {
    // 1) Get user
    const userResp = await new Promise((res, rej) => {
      console.log('Calling GetUser with request:', userId);
      userClient.GetUser({ userId }, (err, resp) => err ? rej(err) : res(resp));
    });
    if (userResp.status !== 'success' || userResp.users.length === 0) {
      throw { code: grpc.status.NOT_FOUND, msg: `Usuario ${userId} no encontrado` };
    }
    const user = userResp.users[0];

    // 2) Get products
    const products = await Promise.all(
      productIds.map(id => new Promise((res, rej) => {
        productClient.GetProduct({ id }, (err, prod) => {
          if (err)       return rej({ code: grpc.status.UNAVAILABLE, msg: `Producto ${id} no disponible` });
          if (!prod.price) return rej({ code: grpc.status.NOT_FOUND,    msg: `Producto ${id} inválido` });
          res(prod);
        });
      }))
    );

    // 3) Calculate total
    const total = products.reduce((sum, p) => sum + p.price, 0);

    // 4) Check and update balance
    if (user.balance < total) {
      throw { code: grpc.status.FAILED_PRECONDITION, msg: `Saldo insuficiente (${user.balance} < ${total})` };
    }
    await new Promise((res, rej) => {
      userClient.UpdateUserBalance(
        { userId, newBalance: user.balance - total },
        (err, updResp) => {
          if (err) {
            const code = err.code === grpc.status.FAILED_PRECONDITION
              ? grpc.status.FAILED_PRECONDITION
              : grpc.status.UNAVAILABLE;
            return rej({ code, msg: err.message });
          }
          res(updResp);
        }
      );
    });

    // 5) Create order
    const order = createOrder({ userId, productIds, total });
    return callback(null, order);

  } catch (err) {
    const code    = err.code    || grpc.status.INTERNAL;
    const message = err.msg     || err.message || 'Error interno';
    return callback({ code, message });
  }
}

function getOrderHandler(call, callback) {
  const order = getOrder(call.request.id);
  if (!order) {
    return callback({ code: grpc.status.NOT_FOUND, message: 'Pedido no encontrado' });
  }
  return callback(null, order);
}

// === RabbitMQ Consumer ===

async function startRabbitMQConsumer() {
  const conn    = await amqp.connect(RABBITMQ_URL);
  const channel = await conn.createChannel();
  await channel.assertQueue(ORDER_QUEUE, { durable: true });

  // 1) Sólo un mensaje “in flight” a la vez
  await channel.prefetch(1);

  console.log(`[*] Esperando mensajes en la cola ${ORDER_QUEUE}…`);

  channel.consume(
    ORDER_QUEUE,
    async (msg) => {
      if (!msg) return;
      const content = msg.content.toString();
      console.log(`[x] Recibido: ${content}`);

      let data;
      try {
        data = JSON.parse(content);
      } catch (e) {
        console.error('[!] JSON inválido, ack:', e.message);
        return channel.ack(msg);
      }

      if (!data.userId || !Array.isArray(data.productIds)) {
        console.warn('[!] Payload inválido, ack para evitar reintentos:', data);
        return channel.ack(msg);
      }

      try {
        // Ejecuta tu lógica gRPC...
        await new Promise((res, rej) => {
          createOrderHandler({ request: data }, (err, resp) => err ? rej(err) : res(resp));
        });
        channel.ack(msg);
        console.log('[✔] Mensaje procesado y ackeado');

      } catch (err) {
        const retryable = [ grpc.status.UNAVAILABLE, grpc.status.INTERNAL ]
          .includes(err.code);

        if (retryable) {
          const backoffMs = 5000; // p.ej. 5 segundos
          console.error(`[!] Retryable error, requeue con back‑off ${backoffMs}ms:`, err.message);
          // Esperamos unos segundos antes de nack para no "spamear" RabbitMQ
          setTimeout(() => {
            channel.nack(msg, false, true);
          }, backoffMs);

        } else {
          console.error('[!] Error no retryable, ack:', err.message);
          channel.ack(msg);
        }
      }
    },
    { noAck: false }
  );
}
// === Arranque combinado ===

function main() {
  const server = new grpc.Server();
  server.addService(orderProto.OrderService.service, {
    CreateOrder: createOrderHandler,
    GetOrder:    getOrderHandler,
  });

  server.bindAsync(ADDRESS, grpc.ServerCredentials.createInsecure(), (err) => {
    if (err) {
      console.error('❌ Error levantando gRPC:', err.message);
      process.exit(1);
    }
    console.log(`🟢 OrderService escuchando en ${ADDRESS}`);
    server.start();
    startRabbitMQConsumer();
  });
}

main();
