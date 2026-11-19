import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const paid = new Counter('orders_paid');
const confirmFailed = new Counter('confirm_failed');

export const options = {
  scenarios: {
    payment: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 30),
      duration: __ENV.DURATION || '60s',
    },
  },
  thresholds: {
    'http_req_duration{name:order}': ['p(95)>=0'],
    'http_req_duration{name:confirm}': ['p(95)>=0'],
  },
};

export default function () {
  const memberId = 1 + Math.floor(Math.random() * 100);
  const productId = 1 + Math.floor(Math.random() * 200);
  const key = `${__VU}-${__ITER}-${Date.now()}`;
  const headers = { 'Content-Type': 'application/json', 'X-Member-Id': String(memberId) };

  const orderRes = http.post(
    `${BASE_URL}/api/orders`,
    JSON.stringify({ items: [{ productId: productId, quantity: 1 }] }),
    { headers: Object.assign({ 'Idempotency-Key': key }, headers), tags: { name: 'order' } }
  );
  if (!check(orderRes, { 'order 201': (r) => r.status === 201 })) {
    return;
  }
  const order = orderRes.json();

  const confirmRes = http.post(
    `${BASE_URL}/api/payments/confirm`,
    JSON.stringify({ orderNo: order.orderNo, paymentKey: `pay-${key}`, amount: order.payAmount }),
    { headers: headers, tags: { name: 'confirm' } }
  );
  if (check(confirmRes, { 'confirm 200': (r) => r.status === 200 })) {
    paid.add(1);
  } else {
    confirmFailed.add(1);
  }
}