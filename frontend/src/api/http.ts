
import axios, { type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types'

interface SilentConfig extends InternalAxiosRequestConfig {
  silent?: boolean
}

const http = axios.create({ baseURL: '/api', timeout: 15000, withCredentials: true })
http.interceptors.response.use(
  ((response: AxiosResponse) => {
    const body = response.data as ApiResponse<unknown>
    if (body?.code !== 0) {
      const config = response.config as SilentConfig
      if (!config?.silent) ElMessage.error(body?.message || '请求失败')
      return Promise.reject(new Error(body?.message || '请求失败'))
    }
    return body.data
  }) as any,
  error => {
    const message = error?.response?.data?.message || error?.message || '网络异常，请检查后端是否启动'
    const config = error.config as SilentConfig
    if (!config?.silent) ElMessage.error(message)
    return Promise.reject(error)
  }
)
export default http
