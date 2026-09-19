
import type { OrderStatus } from '@/types'
export const money = (value?: number) => `¥${Number(value ?? 0).toFixed(2)}`
export const dateTime = (value?: string) => value ? value.replace('T', ' ').slice(0, 19) : '-'
export const orderStatusType = (status: OrderStatus) => ({ PENDING_REVIEW:'warning',PENDING_PAYMENT:'warning',PENDING_ACCEPT:'warning',TO_PACK:'primary',TO_DISPATCH:'info',DELIVERING:'primary',COMPLETED:'success',CANCELED:'danger',REVIEW_REJECTED:'danger',CLOSED_TIMEOUT:'info',CLOSED_STOCK_SHORTAGE:'danger',REFUNDING:'warning',REFUNDED:'info' }[status] || 'info') as any
export const statusOptions = [
  ['PENDING_REVIEW','待处方审核'],['PENDING_PAYMENT','待支付'],['TO_PACK','待打包'],['TO_DISPATCH','待派送'],['DELIVERING','配送中'],['COMPLETED','已完成'],['CANCELED','已取消'],['REVIEW_REJECTED','处方驳回'],['CLOSED_TIMEOUT','超时关闭'],['CLOSED_STOCK_SHORTAGE','库存不足关闭']
]
export const fullAddress = (a: {province?: string;city?:string;district?:string;detailAddress:string}) => `${a.province || ''}${a.city || ''}${a.district || ''}${a.detailAddress || ''}`
