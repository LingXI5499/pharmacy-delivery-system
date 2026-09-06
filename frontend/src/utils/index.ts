
import type { OrderStatus } from '@/types'
export const money = (value?: number) => `¥${Number(value ?? 0).toFixed(2)}`
export const dateTime = (value?: string) => value ? value.replace('T', ' ').slice(0, 19) : '-'
export const orderStatusType = (status: OrderStatus) => ({ PENDING_ACCEPT:'warning', TO_PACK:'primary', TO_DISPATCH:'info', DELIVERING:'primary', COMPLETED:'success', CANCELED:'danger' }[status] || 'info') as any
export const statusOptions = [
  ['PENDING_ACCEPT','待接单'], ['TO_PACK','待打包'], ['TO_DISPATCH','待派送'], ['DELIVERING','配送中'], ['COMPLETED','已完成'], ['CANCELED','已取消']
]
export const fullAddress = (a: {province?: string;city?:string;district?:string;detailAddress:string}) => `${a.province || ''}${a.city || ''}${a.district || ''}${a.detailAddress || ''}`
