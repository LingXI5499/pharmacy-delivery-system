import { expect, test } from '@playwright/test'
import { loginViaUi } from './support/auth'

test('guest visiting cart is redirected to login', async ({ page }) => {
  await page.goto('/cart')

  await expect(page).toHaveURL(/\/login\?redirect=\/cart$/)
  await expect(page.getByRole('heading', { name: '欢迎回来' })).toBeVisible()
})

test('demo user can create and pay an otc order', async ({ page }) => {
  await loginViaUi(page, 'e1_user', 'test123456', /\/home$/)

  const medicineId = await page.evaluate(async () => {
    const response = await fetch('/api/public/medicines?page=1&size=100')
    const payload = await response.json()
    const match = payload?.data?.records?.find((item: { medicineName: string; id: number }) => item.medicineName === 'E1 OTC 感冒灵颗粒')
    return match?.id ?? null
  })

  expect(medicineId).not.toBeNull()

  await page.goto(`/medicine/${medicineId}`)
  await page.getByRole('button', { name: '立即结算' }).click()
  await expect(page).toHaveURL(/\/cart$/)

  await page.getByRole('button', { name: /去结算/ }).click()
  await expect(page).toHaveURL(/\/checkout\?ids=/)

  await page.getByRole('button', { name: '提交订单' }).click()
  await expect(page).toHaveURL(/\/orders\/\d+$/)

  await page.getByRole('button', { name: '模拟支付' }).click()
  await expect(page.getByText('模拟支付成功，库存已确认出库')).toBeVisible()
  await expect(page.getByRole('button', { name: '模拟支付' })).toHaveCount(0)
})

test('purchaser cannot access warehouse workbench', async ({ page }) => {
  await loginViaUi(page, 'e1_purchaser', 'test123456', /\/purchaser$/)
  await page.goto('/warehouse')
  await expect(page).toHaveURL(/\/home$/)
})

test('user cannot access admin dashboard', async ({ page }) => {
  await loginViaUi(page, 'e1_user', 'test123456', /\/home$/)
  await page.goto('/admin/dashboard')
  await expect(page).toHaveURL(/\/home$/)
})
