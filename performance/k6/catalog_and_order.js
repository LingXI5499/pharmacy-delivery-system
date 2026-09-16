import http from 'k6/http'
import { check, sleep } from 'k6'
import { Trend, Rate, Counter } from 'k6/metrics'

const BASE_URL = __ENV.BASE_URL || 'http://127.0.0.1:18093'
const USER_COUNT = Number(__ENV.USER_COUNT || 100)
const PASSWORD = __ENV.P1_PASSWORD || 'test123456'

const catalogTrend = new Trend('catalog_read_ms')
const orderTrend = new Trend('order_create_ms')
const orderListTrend = new Trend('order_list_ms')
const businessOk = new Rate('business_ok')
const ordersCreated = new Counter('orders_created')

export const options = {
  setupTimeout: '8m',
  scenarios: {
    mixed: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 100),
      duration: __ENV.DURATION || '10m'
    }
  },
  thresholds: {
    catalog_read_ms: ['p(95)<500'],
    order_create_ms: ['p(95)<1000'],
    business_ok: ['rate>0.99']
  }
}

function jsonHeaders(token) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  return headers
}

function parseBody(res) {
  try {
    return JSON.parse(res.body)
  } catch (error) {
    return null
  }
}

function loginUser(username) {
  const loginRes = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ username, password: PASSWORD }),
    { headers: jsonHeaders() }
  )
  const loginBody = parseBody(loginRes)
  if (loginRes.status !== 200 || loginBody?.code !== 0 || !loginBody?.data?.accessToken) {
    throw new Error(`login failed for ${username}: HTTP ${loginRes.status} ${loginRes.body}`)
  }
  const token = loginBody.data.accessToken
  const addrRes = http.get(`${BASE_URL}/api/user/addresses`, { headers: jsonHeaders(token) })
  const addrBody = parseBody(addrRes)
  const addressId = addrBody?.data?.[0]?.id
  if (!addressId) {
    throw new Error(`address missing for ${username}`)
  }
  return { token, addressId }
}

export function setup() {
  const catalogRes = http.get(`${BASE_URL}/api/public/medicines?page=1&size=12&keyword=${encodeURIComponent('P1 HOT')}`)
  const catalogBody = parseBody(catalogRes)
  const match = catalogBody?.data?.records?.find((item) => item.medicineName === 'P1 HOT 压测装量')
  if (!match) {
    throw new Error('hot medicine P1 HOT 压测装量 was not found')
  }
  const users = []
  for (let index = 1; index <= USER_COUNT; index += 1) {
    const username = `p1_user_${String(index).padStart(3, '0')}`
    users.push(loginUser(username))
    sleep(3.5)
  }
  return { hotMedicineId: match.id, users }
}

export default function (data) {
  const user = data.users[(__VU - 1) % data.users.length]
  const roll = Math.random()
  if (roll < 0.75) {
    readCatalog()
  } else if (roll < 0.90) {
    listOrders(user.token)
  } else {
    createOrder(user.token, user.addressId, data.hotMedicineId)
  }
  sleep(0.2)
}

function readCatalog() {
  const page = (__ITER % 40) + 1
  const mode = __ITER % 3
  let path = `/api/public/medicines?page=${page}&size=12`
  if (mode === 1) {
    path += `&keyword=${encodeURIComponent('P1 Catalog 12')}`
  } else if (mode === 2) {
    path += '&sort=priceAsc'
  }
  const res = http.get(`${BASE_URL}${path}`)
  catalogTrend.add(res.timings.duration)
  const body = parseBody(res)
  const ok = check(res, {
    'catalog http 200': (r) => r.status === 200,
    'catalog code 0': () => body?.code === 0
  })
  businessOk.add(ok)
}

function listOrders(token) {
  const res = http.get(`${BASE_URL}/api/user/orders?page=1&size=10`, { headers: jsonHeaders(token) })
  orderListTrend.add(res.timings.duration)
  const body = parseBody(res)
  const ok = check(res, {
    'order list http 200': (r) => r.status === 200,
    'order list code 0': () => body?.code === 0
  })
  businessOk.add(ok)
}

function createOrder(token, addressId, hotMedicineId) {
  const addRes = http.post(
    `${BASE_URL}/api/user/cart/items`,
    JSON.stringify({ medicineId: hotMedicineId, quantity: 1 }),
    { headers: jsonHeaders(token) }
  )
  const addBody = parseBody(addRes)
  const addOk = addRes.status === 200 && addBody?.code === 0
  if (!addOk) {
    businessOk.add(false)
    return
  }

  const cartRes = http.get(`${BASE_URL}/api/user/cart`, { headers: jsonHeaders(token) })
  const cartBody = parseBody(cartRes)
  const item = cartBody?.data?.items?.find((entry) => entry.medicineId === hotMedicineId)
  if (!item?.cartItemId) {
    businessOk.add(false)
    return
  }

  const idempotencyKey = `p1-${__VU}-${__ITER}-${Date.now()}`
  const orderRes = http.post(
    `${BASE_URL}/api/user/orders`,
    JSON.stringify({ addressId, cartItemIds: [item.cartItemId], userRemark: 'P1 k6' }),
    { headers: { ...jsonHeaders(token), 'Idempotency-Key': idempotencyKey } }
  )
  orderTrend.add(orderRes.timings.duration)
  const orderBody = parseBody(orderRes)
  const ok = check(orderRes, {
    'order http 200': (r) => r.status === 200,
    'order code 0': () => orderBody?.code === 0 && !!orderBody?.data?.orderId
  })
  businessOk.add(ok)
  if (ok) {
    ordersCreated.add(1)
  }
}
