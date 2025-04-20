import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import logger from './logger.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const ordersFilePath = path.join(__dirname, 'orders.json');

let ordersData = {};

// Función para cargar los datos
function loadOrdersFromFile() {
  try {
    if (fs.existsSync(ordersFilePath)) {
      // Verifica si el archivo existe
      const fileContent = fs.readFileSync(ordersFilePath, 'utf-8');
      ordersData = JSON.parse(fileContent);
      console.log('✔️ Datos de órdenes cargados desde orders.json');
      logger.info('✔️ Datos de órdenes cargados desde orders.json');
    } else {
      console.warn(
        '⚠️ Archivo orders.json no encontrado, iniciando con datos vacíos.'
      );
      ordersData = {}; // Inicia vacío si no existe
    }
  } catch (error) {
    console.error(`❌ Error al cargar/parsear orders.json: ${error.message}`);
    // Considera qué hacer aquí. Salir puede ser drástico.
    process.exit(1);
  }
}

// Función para guardar los datos (¡Importante!)
function saveOrdersToFile() {
  try {
    // Convierte el objeto JS a string JSON (con indentación para legibilidad)
    const dataToSave = JSON.stringify(ordersData, null, 2);
    // Escribe síncronamente al archivo (sobrescribe el contenido anterior)
    fs.writeFileSync(ordersFilePath, dataToSave, 'utf-8');
    console.log('💾 Datos de órdenes guardados en orders.json');
  } catch (error) {
    console.error(`❌ Error al guardar orders.json: ${error.message}`);
    // Aquí podrías tener lógica de reintento o logging más avanzado
  }
}

// Carga los datos al iniciar
loadOrdersFromFile();

// Exporta la variable (que ahora se actualiza)
export const orders = ordersData;

export function createOrder({ userId, productIds,total }) {
  const id = String(Date.now());
  const order = {
    id,
    userId,
    productIds,
    total,
    status: 'CREATED',
  };
  orders[id] = order;
  saveOrdersToFile();
  return order;
}

// Retrieve an order by id.
export function getOrder(id) {
  // Tus logs de depuración aquí (son útiles)
  console.log('las ordenes son ', orders);
  console.log('entramos en getOrder con id:', id);
  console.log('Objeto recuperado:', orders[id]);
  console.log('====');
  // console.log(orders['1']) // Quita esto si ya no lo necesitas para depurar

  // Devuelve la orden correspondiente al ID solicitado
  return orders[id] || null; // Retorna null si no se encuentra
}
