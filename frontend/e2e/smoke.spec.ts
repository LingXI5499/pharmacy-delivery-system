import { expect, test } from '@playwright/test'

test('guest visiting cart is redirected to login', async ({ page }) => {
  await page.goto('/cart')

  await expect(page).toHaveURL(/\/login\?redirect=%2Fcart$/)
  await expect(page.getByRole('heading', { name: '欢迎回来' })).toBeVisible()
})

test('demo user can create and pay an otc order', async ({ page }) => {
  await page.goto('/login')
  await page.getByPlaceholder('请输入账号').fill('e1_user')
  await page.getByPlaceholder('请输入密码').fill('test123456')
  await page.getByRole('button', { name: '登录系统' }).click()

  await expect(page).toHaveURL(/\/home$/)

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
  await page.goto('/login')
  await page.getByPlaceholder('请输入账号').fill('e1_purchaser')
  await page.getByPlaceholder('请输入密码').fill('test123456')
  await page.getByRole('button', { name: '登录系统' }).click()

  await expect(page).toHaveURL(/\/purchaser$/)
  await page.goto('/warehouse')
  await expect(page).toHaveURL(/\/home$/)
})
