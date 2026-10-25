import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getCurrentMenus } from '@/api/system'

/**
 * 动态菜单：登录后从 /api/system/menu/current 拉取
 */
export const useMenuStore = defineStore('menu', () => {
  const menus = ref([])
  const loaded = ref(false)

  async function loadMenus() {
    const res = await getCurrentMenus()
    menus.value = res.data || []
    loaded.value = true
  }

  function reset() {
    menus.value = []
    loaded.value = false
  }

  return { menus, loaded, loadMenus, reset }
})
