export const orders = {};

export function createOrder({ user_id, product_ids, total }) {
  const id = String(Date.now()); // ID simple con timestamp
  const order = {
    id,
    user_id,
    product_ids,
    total,
    status: 'CREATED',
  };
  orders[id] = order;
  return order;
}

export function getOrder(id) {
  return orders[id] || null;
}
