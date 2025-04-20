import grpc from '@grpc/grpc-js';
import protoLoader from '@grpc/proto-loader';
import path from 'path';
import { fileURLToPath } from 'url';
import dotenv from 'dotenv';
import amqp from 'amqplib';

dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname  = path.dirname(__filename);

// === Configuración gRPC ===
const PROTO_PATH = path.join(__dirname, process.env.PROTO_PATH);
const HOST       = process.env.PRODUCT_SERVICE_HOST || '0.0.0.0';
const PORT       = process.env.PRODUCT_SERVICE_PORT || '50053';
const ADDRESS    = `${HOST}:${PORT}`;

// === Configuración RabbitMQ ===
const RABBITMQ_URL  = process.env.RABBITMQ_URL  || 'amqp://guest:guest@localhost:5672';
const PRODUCT_QUEUE = process.env.PRODUCT_QUEUE || 'product-service-queue';

// === Base de datos simulada ===
const products = {
  1: { id:'1', name:'Silla ergonómica', description:'Ideal para oficina', price:199 },
  2: { id:'2', name:'Mesa de madera',    description:'Home office',       price:249.5 },
  3: { id:'3', name:'Lámpara LED',       description:'Luz ajustable',      price:39.9 },
  4: { id:'4', name:'Estantería modular',description:'5 niveles',         price:89.0 },
};

// === Lógica del servicio ===
function getProduct(call, callback) {
  const product = products[call.request.id];
  if (product) {
    callback(null, product);
  } else {
    // Not found → devolvemos error no retryable
    callback({ code: grpc.status.NOT_FOUND, message: 'Producto no encontrado' });
  }
}

// === Consumidor RabbitMQ para reprocesar mensajes en backlog ===
// === Consumidor RabbitMQ para reprocesar mensajes en backlog ===
async function startRabbitMQConsumer() {
  try {
    const conn    = await amqp.connect(RABBITMQ_URL);
    const channel = await conn.createChannel();

    await channel.assertQueue(PRODUCT_QUEUE, { durable: true });
    await channel.prefetch(1);

    console.log(`[*] ProductService RabbitMQ consumer esperando mensajes en ${PRODUCT_QUEUE}`);

    channel.consume(
      PRODUCT_QUEUE,
      async (msg) => {
        if (!msg) return;
        const content = msg.content.toString().trim();
        console.log(`[x] Mensaje en cola ${PRODUCT_QUEUE}: ${content}`);

        let data;
        // 1) Intentamos parsear JSON
        try {
          data = JSON.parse(content);
        } catch (e) {
          // 2) Si no es JSON, miramos si es ruta "/api/products/123"
          const m = content.match(/^\/api\/products\/(\d+)$/);
          if (m) {
            data = { id: m[1] };
            console.log(`↪ Interpretada ruta como objeto:`, data);
          } else {
            console.error('[!] JSON inválido y no es ruta, ack:', e.message);
            return channel.ack(msg);
          }
        }

        // Validación mínima
        if (!data.id) {
          console.warn('[!] Payload sin campo id, ack para descartar:', data);
          return channel.ack(msg);
        }

        // 3) Procesamos con la misma lógica gRPC interna
        try {
          await new Promise((res, rej) => {
            getProduct({ request: { id: data.id } }, (err, product) => {
              if (err) return rej(err);
              console.log('✅ Producto reprocesado desde cola:', product);
              res();
            });
          });
          channel.ack(msg);
        } catch (err) {
          const retryable = err.code === grpc.status.UNAVAILABLE;
          if (retryable) {
            console.error('[!] Servicio unavailable, requeue:', err.message);
            channel.nack(msg, false, true);
          } else {
            console.error('[!] Error no retryable, ack y descarta:', err.message);
            channel.ack(msg);
          }
        }
      },
      { noAck: false }
    );

  } catch (err) {
    console.error('[!] Error al conectar RabbitMQ en ProductService:', err.message);
    setTimeout(startRabbitMQConsumer, 5000);
  }
}



// === Iniciar servidor gRPC y consumidor ===
function main() {
  // 1) gRPC
  const pkgDef      = protoLoader.loadSync(PROTO_PATH);
  const productDef  = grpc.loadPackageDefinition(pkgDef).product;
  const server      = new grpc.Server();
  server.addService(productDef.ProductService.service, { GetProduct: getProduct });
  server.bindAsync(ADDRESS, grpc.ServerCredentials.createInsecure(), () => {
    console.log(`🟢 ProductService gRPC escuchando en ${ADDRESS}`);
    server.start();
  });

  // 2) RabbitMQ
  startRabbitMQConsumer();
}

main();