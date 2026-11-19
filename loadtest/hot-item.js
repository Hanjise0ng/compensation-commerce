import http from 'k6/http';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const created = new Counter('order_created');
const outOfStock = new Counter('order_out_of_stock');
const conflict = new Counter('order_conflict');
const otherError = new Counter('order_other_error');

export const options = {
  scenarios: {
    hot: {
      executor: 'shared-iterations',
      vus: Number(__ENV.VUS || 50),
      iterations: Number(__ENV.ITERATIONS || 2000),
      maxDuration: '5m',
    },
  },
};

export default function () {
  const memberId = 1 + Math.floor(Math.random() * 100);
  const key = `${__VU}-${__ITER}-${Date.now()}`;
  const res = http.post(
    `${BASE_URL}/api/orders`,
    JSON.stringify({ items: [{ productId: 201, quantity: 1 }] }),
    { headers: { 'Content-Type': 'application/json', 'X-Member-Id': String(memberId), 'Idempotency-Key': key } }
  );

  if (res.status === 201) {
    created.add(1);
    return;
  }
  let title = '';
  try {
    title = res.json('title');
  } catch (e) {
    title = '';
  }
  if (title === 'OUT_OF_STOCK') {
    outOfStock.add(1);
  } else if (title === 'CONCURRENT_MODIFICATION') {
    conflict.add(1);
  } else {
    otherError.add(1);
  }
}