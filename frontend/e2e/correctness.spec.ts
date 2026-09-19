import { expect, test } from '@playwright/test'
import { apiJson, apiLogin, loginViaUi } from './support/auth'
import { mysqlExec, mysqlRows } from './support/mysql'
import { payCurrentOrder, submitCatalogOrder } from './support/shop'

test('fefo checkout reserves nearer expiry batch first then later batch', async ({ page }) => {
  await loginViaUi(page, 'e1_user', 'test123456', /\/home$/)
  const orderId = await submitCatalogOrder(page, 'E1 FEFO 维生素C', 3)

  expect(mysqlRows(`
    SELECT b.batch_no, r.quantity
    FROM inventory_reservation r
    JOIN medicine_batch b ON b.id = r.batch_id
    WHERE r.order_id = ${orderId}
    ORDER BY b.expiry_date ASC, b.id ASC
  `)).toEqual([
    ['E1-FEFO-EARLY', '2'],
    ['E1-FEFO-LATE', '1']
  ])

  await payCurrentOrder(page)
  await expect(page.locator('section.hero .el-tag')).toContainText('待打包')

  expect(mysqlRows(`
    SELECT b.batch_no, l.reserved_delta
    FROM inventory_ledger l
    JOIN medicine_batch b ON b.id = l.batch_id
    WHERE l.business_type = 'SALE_COMMIT' AND l.business_id = '${orderId}'
    ORDER BY b.expiry_date ASC, b.id ASC
  `)).toEqual([
    ['E1-FEFO-EARLY', '-2'],
    ['E1-FEFO-LATE', '-1']
  ])
  expect(mysqlExec(`SELECT available_qty FROM medicine_batch WHERE batch_no='E1-FEFO-EARLY'`)).toBe('0')
  expect(mysqlExec(`SELECT available_qty FROM medicine_batch WHERE batch_no='E1-FEFO-LATE'`)).toBe('3')
})

test('unpaid order closes after payment timeout and releases stock', async ({ page }) => {
  test.setTimeout(90_000)
  await loginViaUi(page, 'e1_user', 'test123456', /\/home$/)
  const orderId = await submitCatalogOrder(page, 'E1 TIMEOUT 口罩')
  await expect(page.locator('section.hero .el-tag')).toContainText('待支付')
  expect(mysqlExec(`SELECT reserved_qty FROM medicine_batch WHERE batch_no='E1-TIMEOUT-001'`)).toBe('1')

  await expect.poll(
    () => mysqlExec(`SELECT order_status FROM pharmacy_order WHERE id=${orderId}`),
    { timeout: 50_000 }
  ).toBe('CLOSED_TIMEOUT')

  await page.reload()
  await expect(page.locator('section.hero .el-tag')).toContainText('超时关闭')
  await expect(page.getByRole('button', { name: '模拟支付' })).toHaveCount(0)
  expect(mysqlExec(`SELECT available_qty, reserved_qty FROM medicine_batch WHERE batch_no='E1-TIMEOUT-001'`)).toBe('1\t0')
  expect(mysqlExec(`SELECT COUNT(*) FROM inventory_ledger WHERE business_type='ORDER_RELEASE' AND business_id='${orderId}'`)).toBe('1')
})

test('paid unpackaged order can refund and restock original batch', async ({ page, request }) => {
  await loginViaUi(page, 'e1_user', 'test123456', /\/home$/)
  const orderId = await submitCatalogOrder(page, 'E1 REFUND 碘伏')
  await payCurrentOrder(page)
  await expect(page.locator('section.hero .el-tag')).toContainText('待打包')
  expect(mysqlExec(`SELECT available_qty FROM medicine_batch WHERE batch_no='E1-REFUND-001'`)).toBe('1')

  const userToken = await apiLogin(request, 'e1_user', 'test123456')
  const refund = await apiJson<{ refundNo: string; amount: number | string }>(
    request,
    userToken,
    'POST',
    `/api/user/orders/${orderId}/refunds`,
    { reason: 'E1 未发货退款演练' }
  )
  await page.reload()
  await expect(page.locator('section.hero .el-tag')).toContainText('退款中')

  const adminToken = await apiLogin(request, 'e1_admin', 'test123456')
  await apiJson(request, adminToken, 'POST', '/api/admin/mock-refunds/callback', {
    refundNo: refund.refundNo,
    callbackKey: `e1-refund-${orderId}`,
    success: true,
    amount: Number(refund.amount)
  })

  await page.reload()
  await expect(page.locator('section.hero .el-tag')).toContainText('已退款')
  expect(mysqlExec(`SELECT available_qty FROM medicine_batch WHERE batch_no='E1-REFUND-001'`)).toBe('2')
  expect(mysqlExec(`SELECT COUNT(*) FROM inventory_ledger WHERE business_type='REFUND_RESTOCK' AND business_id='${orderId}'`)).toBe('1')
  expect(mysqlExec(`SELECT status FROM refund_record WHERE order_id=${orderId}`)).toBe('SUCCESS')
})
