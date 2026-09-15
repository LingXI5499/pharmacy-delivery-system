import axios, { type AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types'

interface RequestConfig extends InternalAxiosRequestConfig { silent?: boolean; _retried?: boolean }
let accessToken: string | null = null
let refreshing: Promise<string | null> | null = null

export const setAccessToken = (value: string | null) => { accessToken = value }
const http = axios.create({ baseURL: '/api', timeout: 15000, withCredentials: true })
const refreshClient = axios.create({ baseURL: '/api', timeout: 15000, withCredentials: true })

http.interceptors.request.use(config => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`
  return config
})

async function refreshAccessToken(): Promise<string | null> {
  try {
    const response = await refreshClient.post<ApiResponse<{accessToken:string}>>('/auth/refresh')
    const token = response.data.data?.accessToken || null
    setAccessToken(token)
    return token
  } catch {
    setAccessToken(null)
    return null
  }
}

http.interceptors.response.use(
  ((response: AxiosResponse) => {
    if (response.config.responseType === 'blob') return response.data
    const body = response.data as ApiResponse<unknown>
    if (body?.code !== 0) {
      const config = response.config as RequestConfig
      if (!config?.silent) ElMessage.error(body?.message || '请求失败')
      return Promise.reject(new Error(body?.message || '请求失败'))
    }
    return body.data
  }) as any,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const config = error.config as RequestConfig | undefined
    if (error.response?.status === 401 && config && !config._retried && !config.url?.includes('/auth/refresh')) {
      config._retried = true
      refreshing ||= refreshAccessToken().finally(() => { refreshing = null })
      const token = await refreshing
      if (token) { config.headers.Authorization = `Bearer ${token}`; return http.request(config) }
    }
    const message = error.response?.data?.message || error.message || '网络异常，请检查后端是否启动'
    if (!config?.silent) ElMessage.error(message)
    return Promise.reject(error)
  }
)
export default http
