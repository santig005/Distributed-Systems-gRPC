import grpc from '@grpc/grpc-js';
import protoLoader from '@grpc/proto-loader';
import path from 'path';
import { fileURLToPath } from 'url';
import dotenv from 'dotenv';

dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const PROTO_PATH = path.join(__dirname, process.env.PROTO_PATH);
const HOST = process.env.PRODUCT_SERVICE_HOST || '0.0.0.0';
const PORT = process.env.PRODUCT_SERVICE_PORT || '50053';
const ADDRESS = `${HOST}:${PORT}`;

// Cargar el proto
const packageDefinition = protoLoader.loadSync(PROTO_PATH);
const productProto = grpc.loadPackageDefinition(packageDefinition).product;

// Base de datos simulada
const products = {
  1: {
    id: '1',
    name: 'Silla ergonómica',
    description: 'Ideal para oficina y largas jornadas de trabajo',
    price: 199,
  },
  2: {
    id: '2',
    name: 'Mesa de madera',
    description: 'Escritorio amplio y robusto para home office',
    price: 249.5,
  },
  3: {
    id: '3',
    name: 'Lámpara LED',
    description: 'Luz blanca ajustable con brazo flexible',
    price: 39.9,
  },
  4: {
    id: '4',
    name: 'Estantería modular',
    description: 'Organizador de libros y documentos de 5 niveles',
    price: 89.0,
  },
};

// Lógica del servicio
function getProduct(call, callback) {
  const product = products[call.request.id];
  if (product) {
    callback(null, product);
  } else {
    callback(new Error('Producto no encontrado'));
  }
}

// Iniciar servidor
function main() {
  const server = new grpc.Server();
  server.addService(productProto.ProductService.service, {
    GetProduct: getProduct,
  });

  server.bindAsync(ADDRESS, grpc.ServerCredentials.createInsecure(), () => {
    console.log(`🟢 ProductService escuchando en ${ADDRESS}`);
    server.start();
  });
}

main();
