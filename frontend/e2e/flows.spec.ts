import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { expect, test } from '@playwright/test'
import { loginViaUi } from './support/auth'

const fixturesDir = path.dirname(fileURLToPath(import.meta.url))
const rxFixture = path.join(fixturesDir, 'fixtures', 'rx-sample.png')

async function findMedicineId(page: import('@playwright/test').Page, medicineName: string) {
  return page.evaluate(async (name) => {
    const response = await fetch('/api/public/medicines?page=1&size=100')
    const payload = await response.json()
    const match = payload?.data?.records?.find((item: { medicineName: string; id: number }) => item.medicineName === name)
    return match?.id ?? null
  }, medicineName)
}

test('prescription order requires pharmacist approval before payment', async ({ browser }) => {
  const userContext = await browser.newContext()
  const userPage = await userContext.newPage()
  await loginViaUi(userPage, 'e1_user', 'test123456', /\/home$/)

  const medicineId = await findMedicineId(userPage, 'E1 RX 阿莫西林胶囊')
  expect(medicineId).not.toBeNull()

  await userPage.goto(`/medicine/${medicineId}`)
  await userPage.getByRole('button', { name: '立即结算' }).click()
  await expect(userPage).toHaveURL(/\/cart$/)
  await userPage.getByRole('button', { name: /去结算/ }).click()
  await expect(userPage).toHaveURL(/\/checkout\?ids=/)
  await userPage.locator('input[type="file"]').setInputFiles(rxFixture)
  await userPage.getByRole('button', { name: '提交订单' }).click()
  await expect(userPage).toHaveURL(/\/orders\/\d+$/)
  await expect(userPage.locator('section.hero .el-tag')).toContainText('待处方审核')
  await expect(userPage.getByRole('button', { name: '模拟支付' })).toHaveCount(0)

  const orderUrl = userPage.url()
  await userContext.close()

  const pharmacistContext = await browser.newContext()
  const pharmacistPage = await pharmacistContext.newPage()
  await loginViaUi(pharmacistPage, 'e1_pharmacist', 'test123456', /\/pharmacist$/)
  await expect(pharmacistPage.getByText('药师审方工作台')).toBeVisible()
  await pharmacistPage.getByRole('button', { name: '批准' }).first().click()
  await expect(pharmacistPage.getByText('审核结果已提交')).toBeVisible()
  await pharmacistContext.close()

  const payContext = await browser.newContext()
  const payPage = await payContext.newPage()
  await loginViaUi(payPage, 'e1_user', 'test123456', /\/home$/)
  await payPage.goto(orderUrl)
  await payPage.getByRole('button', { name: '模拟支付' }).click()
  await expect(payPage.getByText('模拟支付成功，库存已确认出库')).toBeVisible()
  await payContext.close()
})

test('procurement approve and warehouse receive closes the loop', async ({ browser }) => {
  test.setTimeout(120_000)
  const purchaserContext = await browser.newContext()
  const purchaserPage = await purchaserContext.newPage()
  await loginViaUi(purchaserPage, 'e1_purchaser', 'test123456', /\/purchaser$/)

  const supplierCode = `E1-SUP-${Date.now()}`
  await purchaserPage.locator('.el-form-item', { hasText: '编码' }).locator('input').fill(supplierCode)
  await purchaserPage.locator('.el-form-item', { hasText: '名称' }).locator('input').fill('E1 运行时供应商')
  await purchaserPage.locator('.el-form-item', { hasText: '联系人' }).locator('input').fill('E1 联系人')
  await purchaserPage.locator('.el-form-item', { hasText: '电话' }).locator('input').fill('13810000999')
  await purchaserPage.getByRole('button', { name: '新增供应商' }).click()
  await expect(purchaserPage.getByText('供应商已创建')).toBeVisible()

  await purchaserPage.locator('.el-form-item', { hasText: '供应商' }).locator('.el-select').click()
  await purchaserPage.getByRole('option', { name: new RegExp(supplierCode) }).click()
  await purchaserPage.locator('.item-row .el-select').first().click()
  await purchaserPage.getByRole('option', { name: 'E1 OTC 感冒灵颗粒' }).click()
  await purchaserPage.getByRole('button', { name: '提交采购单' }).click()
  await expect(purchaserPage.getByText(/采购单 .* 已提交/)).toBeVisible()
  await purchaserContext.close()

  const adminContext = await browser.newContext()
  const adminPage = await adminContext.newPage()
  await loginViaUi(adminPage, 'e1_admin', 'test123456', /\/admin\/dashboard$/)
  await adminPage.goto('/admin/purchase-orders')
  await expect(adminPage.getByRole('heading', { name: '采购审批' })).toBeVisible()
  await adminPage.locator('.el-table__body').getByRole('button', { name: '批准' }).first().click()
  const confirmDialog = adminPage.locator('.el-message-box')
  await expect(confirmDialog).toBeVisible()
  await confirmDialog.getByRole('button', { name: /确定|OK/ }).click()
  await expect(adminPage.getByText('采购单已批准')).toBeVisible()
  await adminContext.close()

  const warehouseContext = await browser.newContext()
  const warehousePage = await warehouseContext.newPage()
  await loginViaUi(warehousePage, 'e1_warehouse', 'test123456', /\/warehouse$/)
  await warehousePage.getByRole('button', { name: '收货' }).first().click()
  await expect(warehousePage.getByText(/收货录入/)).toBeVisible()

  const tableInputs = warehousePage.locator('.el-table__body-wrapper input')
  await tableInputs.nth(0).fill('E1-RECV-001')
  await tableInputs.nth(1).fill('2026-01-01')
  await tableInputs.nth(1).press('Enter')
  await tableInputs.nth(2).fill('2027-01-01')
  await tableInputs.nth(2).press('Enter')
  await warehousePage.getByRole('button', { name: '提交本批收货' }).click()
  await expect(warehousePage.getByText('分批收货已提交，采购价取自采购单明细')).toBeVisible()
  await warehouseContext.close()
})
