import { expect, type APIRequestContext, type Page } from '@playwright/test'

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

export async function apiLogin(request: APIRequestContext, username: string, password: string) {
  const response = await request.post('/api/auth/login', {
    data: { username, password }
  })
  const body = await response.json()
  expect(response.ok(), `login HTTP status ${response.status()}`).toBeTruthy()
  expect(body.code, body.message || 'api login failed').toBe(0)
  expect(body.data?.accessToken).toBeTruthy()
  return body.data.accessToken as string
}

export async function apiJson<T>(
  request: APIRequestContext,
  token: string,
  method: 'GET' | 'POST',
  url: string,
  data?: unknown
): Promise<T> {
  const response = await request.fetch(url, {
    method,
    headers: { Authorization: `Bearer ${token}` },
    data
  })
  const body = await response.json()
  expect(response.ok(), `${method} ${url} HTTP ${response.status()}`).toBeTruthy()
  expect(body.code, body.message || `${method} ${url} failed`).toBe(0)
  return body.data as T
}
