import { expect, type Page } from '@playwright/test'

export async function findMedicineId(page: Page, medicineName: string): Promise<number> {
  const medicineId = await page.evaluate(async (name) => {
    const response = await fetch('/api/public/medicines?page=1&size=100')
    const payload = await response.json()
    const match = payload?.data?.records?.find((item: { medicineName: string; id: number }) => item.medicineName === name)
    return match?.id ?? null
  }, medicineName)
  expect(medicineId, `catalog missing ${medicineName}`).not.toBeNull()
  return medicineId as number
}

export async function submitCatalogOrder(page: Page, medicineName: string, quantity = 1): Promise<number> {
  const medicineId = await findMedicineId(page, medicineName)
  await page.goto(`/medicine/${medicineId}`)
  if (quantity > 1) {
    for (let i = 1; i < quantity; i += 1) {
      await page.locator('.el-input-number__increase').click()
    }
  }
  await page.getByRole('button', { name: '立即结算' }).click()
  await expect(page).toHaveURL(/\/cart$/)
  await page.getByRole('button', { name: /去结算/ }).click()
  await expect(page).toHaveURL(/\/checkout\?ids=/)
  await page.getByRole('button', { name: '提交订单' }).click()
  await expect(page).toHaveURL(/\/orders\/\d+$/)
  const match = page.url().match(/\/orders\/(\d+)/)
  expect(match).not.toBeNull()
  return Number(match?.[1])
}

export async function checkoutMedicine(page: Page, medicineName: string, quantity = 1) {
  return submitCatalogOrder(page, medicineName, quantity)
}

export async function payCurrentOrder(page: Page) {
  await page.getByRole('button', { name: '模拟支付' }).click()
  await expect(page.getByText('模拟支付成功，库存已确认出库')).toBeVisible()
}
