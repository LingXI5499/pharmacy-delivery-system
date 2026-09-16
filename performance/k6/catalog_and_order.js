import http from 'k6/http'
import { check, sleep } from 'k6'
import { Trend, Rate, Counter } from 'k6/metrics'

const BASE_URL = __ENV.BASE_URL || 'http://127.0.0.1:18093'
const USER_COUNT = Number(__ENV.USER_COUNT || 100)
const PASSWORD = __ENV.P1_PASSWORD || 'test123456'

const catalogTrend = new Trend('catalog_read_ms', true)
const orderTrend = new Trend('order_create_ms', true)
const orderListTrend = new Trend('order_list_ms', true)
const businessOk = new Rate('business_ok')
const ordersCreated = new Counter('orders_created')

export const options = {
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

function username() {
  const index = ((__VU - 1) % USER_COUNT) + 1
  return `p1_user_${String(index).padStart(3, '0')}`
}

let accessToken
let addressId
let hotMedicineId

export function setup() {
  const res = http.get(`${BASE_URL}/api/public/medicines?page=1&size=12&keyword=${encodeURIComponent('P1 HOT')}`)
  const body = parseBody(res)
  const match = body?.data?.records?.find((item) => item.medicineName === 'P1 HOT 压测装量')
  if (!match) {
    throw new Error('hot medicine P1 HOT 压测装量 was not found')
  }
  return { hotMedicineId: match.id }
}

export default function (data) {
  if (!accessToken) {
    const loginRes = http.post(
      `${BASE_URL}/api/auth/login`,
      JSON.stringify({ username: username(), password: PASSWORD }),
      { headers: jsonHeaders() }
    )
    const loginBody = parseBody(loginRes)
    const loginOk = check(loginRes, {
      'login http 200': (r) => r.status === 200,
      'login code 0': () => loginBody?.code === 0 && !!loginBody?.data?.accessToken
    })
    businessOk.add(loginOk)
    if (!loginOk) {
      sleep(1)
      return
    }
    accessToken = loginBody.data.accessToken
    const addrRes = http.get(`${BASE_URL}/api/user/addresses`, { headers: jsonHeaders(accessToken) })
    const addrBody = parseBody(addrRes)
    addressId = addrBody?.data?.[0]?.id
    if (!addressId) {
      businessOk.add(false)
      return
    }
    hotMedicineId = data.hotMedicineId
  }

  const roll = Math.random()
  if (roll < 0.75) {
    readCatalog()
  } else if (roll < 0.90) {
    listOrders()
  } else {
    createOrder()
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

function listOrders() {
  const res = http.get(`${BASE_URL}/api/user/orders?page=1&size=10`, { headers: jsonHeaders(accessToken) })
  orderListTrend.add(res.timings.duration)
  const body = parseBody(res)
  const ok = check(res, {
    'order list http 200': (r) => r.status === 200,
    'order list code 0': () => body?.code === 0
  })
  businessOk.add(ok)
}

function createOrder() {
  const addRes = http.post(
    `${BASE_URL}/api/user/cart/items`,
    JSON.stringify({ medicineId: hotMedicineId, quantity: 1 }),
    { headers: jsonHeaders(accessToken) }
  )
  const addBody = parseBody(addRes)
  const addOk = addRes.status === 200 && addBody?.code === 0
  if (!addOk) {
    businessOk.add(false)
    return
  }

  const cartRes = http.get(`${BASE_URL}/api/user/cart`, { headers: jsonHeaders(accessToken) })
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
    { headers: { ...jsonHeaders(accessToken), 'Idempotency-Key': idempotencyKey } }
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
