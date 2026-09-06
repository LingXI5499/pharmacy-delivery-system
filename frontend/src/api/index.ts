
import http from './http'
import type { Address, Cart, Category, CategoryDistribution, DashboardSummary, HotMedicine, Medicine, Order, OrderDetail, PageData, Rider, TrendPoint, User } from '@/types'

export const authApi = {
  register: (data: any) => http.post<any, User>('/auth/register', data),
  login: (data: any) => http.post<any, {user: User; redirectPath: string}>('/auth/login', data),
  me: () => http.get<any, User>('/auth/me', { silent: true } as any),
  logout: () => http.post<any, void>('/auth/logout')
}
export const publicApi = {
  categories: () => http.get<any, Category[]>('/public/categories'),
  medicines: (params: any) => http.get<any, PageData<Medicine>>('/public/medicines', {params}),
  medicine: (id: number) => http.get<any, Medicine>(`/public/medicines/${id}`)
}
export const cartApi = {
  get: () => http.get<any, Cart>('/user/cart'),
  add: (data: {medicineId:number;quantity:number}) => http.post<any, void>('/user/cart/items',data),
  quantity: (id:number, quantity:number) => http.put<any,void>(`/user/cart/items/${id}`,{quantity}),
  selected: (id:number, selected:boolean) => http.patch<any,void>(`/user/cart/items/${id}/selected`,{selected}),
  remove: (id:number) => http.delete<any,void>(`/user/cart/items/${id}`),
  clearInvalid: () => http.delete<any,number>('/user/cart/invalid-items')
}
export const addressApi = {
  list: () => http.get<any, Address[]>('/user/addresses'),
  add: (data:any) => http.post<any,Address>('/user/addresses',data),
  update: (id:number,data:any) => http.put<any,Address>(`/user/addresses/${id}`,data),
  remove: (id:number) => http.delete<any,void>(`/user/addresses/${id}`),
  setDefault: (id:number) => http.patch<any,void>(`/user/addresses/${id}/default`)
}
export const orderApi = {
  create: (data:any) => http.post<any,{orderId:number;orderNo:string;orderStatus:string;orderAmount:number}>('/user/orders',data),
  page: (params:any) => http.get<any,PageData<Order>>('/user/orders',{params}),
  detail: (id:number) => http.get<any,OrderDetail>(`/user/orders/${id}`),
  cancel: (id:number, reason:string) => http.post<any,void>(`/user/orders/${id}/cancel`,{reason})
}
export const dashboardApi = {
  summary: () => http.get<any,DashboardSummary>('/admin/dashboard/summary'),
  orderTrend: (days=7) => http.get<any,TrendPoint[]>('/admin/dashboard/order-trend',{params:{days}}),
  salesTrend: (days=7) => http.get<any,TrendPoint[]>('/admin/dashboard/sales-trend',{params:{days}}),
  category: (days=7) => http.get<any,CategoryDistribution[]>('/admin/dashboard/category-distribution',{params:{days}}),
  hot: (limit=5) => http.get<any,HotMedicine[]>('/admin/dashboard/hot-medicines',{params:{limit}})
}
export const adminCategoryApi = {
  page: (params:any) => http.get<any,PageData<Category>>('/admin/categories',{params}),
  add:(data:any)=>http.post<any,Category>('/admin/categories',data), update:(id:number,data:any)=>http.put<any,Category>(`/admin/categories/${id}`,data), remove:(id:number)=>http.delete<any,void>(`/admin/categories/${id}`), status:(id:number,status:number)=>http.patch<any,void>(`/admin/categories/${id}/status`,{status})
}
export const adminMedicineApi = {
  page:(params:any)=>http.get<any,PageData<Medicine>>('/admin/medicines',{params}), add:(data:any)=>http.post<any,Medicine>('/admin/medicines',data), update:(id:number,data:any)=>http.put<any,Medicine>(`/admin/medicines/${id}`,data), remove:(id:number)=>http.delete<any,void>(`/admin/medicines/${id}`), stock:(id:number,data:any)=>http.patch<any,void>(`/admin/medicines/${id}/stock`,data), status:(id:number,status:number)=>http.patch<any,void>(`/admin/medicines/${id}/status`,{status}), low:(params:any)=>http.get<any,PageData<Medicine>>('/admin/medicines/low-stock',{params})
}
export const adminOrderApi = {
  page:(params:any)=>http.get<any,PageData<Order>>('/admin/orders',{params}), detail:(id:number)=>http.get<any,OrderDetail>(`/admin/orders/${id}`), accept:(id:number,adminRemark='')=>http.post<any,void>(`/admin/orders/${id}/accept`,{adminRemark}), pack:(id:number,adminRemark='')=>http.post<any,void>(`/admin/orders/${id}/pack`,{adminRemark}), dispatch:(id:number,data:any)=>http.post<any,void>(`/admin/orders/${id}/dispatch`,data), complete:(id:number,adminRemark='')=>http.post<any,void>(`/admin/orders/${id}/complete`,{adminRemark}), cancel:(id:number,reason:string)=>http.post<any,void>(`/admin/orders/${id}/cancel`,{reason})
}
export const riderApi = {
  page:(params:any)=>http.get<any,PageData<Rider>>('/admin/riders',{params}), available:()=>http.get<any,Rider[]>('/admin/riders/available'), add:(data:any)=>http.post<any,Rider>('/admin/riders',data), update:(id:number,data:any)=>http.put<any,Rider>(`/admin/riders/${id}`,data), remove:(id:number)=>http.delete<any,void>(`/admin/riders/${id}`), status:(id:number,status:number)=>http.patch<any,void>(`/admin/riders/${id}/status`,{status})
}
export const adminUserApi = { page:(params:any)=>http.get<any,PageData<User>>('/admin/users',{params}), status:(id:number,status:number)=>http.patch<any,void>(`/admin/users/${id}/status`,{status}) }
