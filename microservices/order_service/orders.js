// orders.js
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

// Set __dirname for ES modules.
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Define the path to the JSON file that will store the orders.
const ordersFilePath = path.join(__dirname, 'orders.json');

// Initialize orders; try to load from file if available.
export let orders = {};

try {
  const data = fs.readFileSync(ordersFilePath, 'utf8');
  orders = JSON.parse(data);
  console.log('Orders loaded from file.');
} catch (err) {
  // If file doesn't exist or there's an error reading/parsing, start with an empty object.
  orders = {};
  console.log('No existing orders file found. Starting fresh.');
}

// Function that writes the current orders to the JSON file.
function saveOrders() {
  fs.writeFileSync(ordersFilePath, JSON.stringify(orders, null, 2));
}

// Create a new order and persist it to the file.
export function createOrder({ user_id, product_ids, total }) {
  // Create a unique order id using timestamp.
  const id = String(Date.now());
  const order = {
    id,
    user_id,
    product_ids,
    total,
    status: 'CREATED',
  };
  orders[id] = order;
  saveOrders();
  return order;
}

// Retrieve an order by id.
export function getOrder(id) {
  return orders[id] || null;
}