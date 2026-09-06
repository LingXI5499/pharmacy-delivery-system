
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { cartApi } from '@/api'
import type { Cart } from '@/types'
export const useCartStore = defineStore('cart',()=>{
 const cart=ref<Cart>({items:[],selectedCount:0,selectedAmount:0}); const count=computed(()=>cart.value.items.reduce((sum,i)=>sum+i.quantity,0));
 const load=async()=>{cart.value=await cartApi.get();return cart.value}; const clear=()=>cart.value={items:[],selectedCount:0,selectedAmount:0};
 return {cart,count,load,clear}
})
