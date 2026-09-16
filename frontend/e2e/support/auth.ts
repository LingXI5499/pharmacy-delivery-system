import { expect, type Page } from '@playwright/test'

export async function loginViaUi(page: Page, username: string, password: string, expectedPath: RegExp) {
  await page.goto('/login')
  await page.getByPlaceholder('请输入账号').fill(username)
  await page.getByPlaceholder('请输入密码').fill(password)

  const loginResponse = page.waitForResponse((response) =>
    response.url().includes('/api/auth/login') && response.request().method() === 'POST'
  )
  await page.getByRole('button', { name: '登录系统' }).click()

  const response = await loginResponse
  const body = await response.json()
  expect(response.ok(), `login HTTP status ${response.status()}`).toBeTruthy()
  expect(body.code, body.message || 'login failed').toBe(0)

  await expect(page).toHaveURL(expectedPath)
}
