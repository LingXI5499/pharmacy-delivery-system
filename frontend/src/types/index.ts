
export interface ApiResponse<T> { code: number; message: string; data: T; timestamp: string }
export interface PageData<T> { records: T[]; current: number; size: number; total: number; pages: number }
export type Role = 'USER' | 'ADMIN'
export type OrderStatus = 'PENDING_ACCEPT' | 'TO_PACK' | 'TO_DISPATCH' | 'DELIVERING' | 'COMPLETED' | 'CANCELED'
export interface User { id: number; username: string; nickname: string; phone?: string; role: Role; status: number }
export interface Category { id: number; categoryName: string; categoryImage?: string; description?: string; sortNo: number; status: number; createTime?: string; updateTime?: string }
export interface Medicine { id: number; categoryId: number; categoryName: string; medicineName: string; imageUrl?: string; description?: string; usageInstruction?: string; precautions?: string; price: number; stock: number; warningStock: number; status: number; isLowStock: boolean; createTime?: string; updateTime?: string }
export interface CartItem { cartItemId: number; medicineId: number; medicineName: string; imageUrl?: string; price: number; stock: number; quantity: number; selected: boolean; subtotalAmount: number; available: boolean }
export interface Cart { items: CartItem[]; selectedCount: number; selectedAmount: number }
export interface Address { id: number; receiverName: string; receiverPhone: string; province?: string; city?: string; district?: string; detailAddress: string; isDefault: number; createTime?: string; updateTime?: string }
export interface Rider { id: number; riderName: string; phone: string; status: number; remark?: string; createTime?: string; updateTime?: string }
export interface Order { id: number; orderNo: string; username?: string; userId: number; receiverName: string; receiverPhone: string; receiverAddress: string; productAmount: number; deliveryFee: number; orderAmount: number; orderStatus: OrderStatus; orderStatusName: string; riderId?: number; riderName?: string; riderPhone?: string; userRemark?: string; adminRemark?: string; cancelReason?: string; createTime: string }
export interface OrderItem { id: number; medicineId?: number; medicineName: string; medicineImage?: string; medicinePrice: number; quantity: number; subtotalAmount: number }
export interface OrderStatusLog { id: number; beforeStatus?: OrderStatus; beforeStatusName?: string; afterStatus: OrderStatus; afterStatusName: string; operatorType: string; operatorId?: number; remark?: string; createTime: string }
export interface OrderDetail extends Order { acceptedTime?: string; packedTime?: string; dispatchedTime?: string; completedTime?: string; canceledTime?: string; items: OrderItem[]; statusLogs: OrderStatusLog[] }
export interface DashboardSummary { todayOrderCount: number; todaySalesAmount: number; pendingOrderCount: number; lowStockCount: number; toAcceptCount: number; toPackCount: number; toDispatchCount: number; deliveringCount: number; completedCount: number }
export interface TrendPoint { date: string; value: number; amount: number }
export interface CategoryDistribution { categoryName: string; amount: number; quantity: number }
export interface HotMedicine { medicineName: string; imageUrl?: string; salesQuantity: number }
