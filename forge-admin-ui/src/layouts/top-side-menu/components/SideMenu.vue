<template>
  <n-menu
    ref="menu"
    class="side-menu"
    accordion
    :indent="10"
    :collapsed-icon-size="22"
    :collapsed-width="64"
    :collapsed="appStore.collapsed"
    :options="sideMenuOptions"
    :value="activeKey"
    @update:value="handleMenuSelect"
  />
</template>

<script setup>
import { useRoute } from 'vue-router'
import { useAppStore, usePermissionStore } from '@/store'
import { useMenu, findTopMenuByPath } from '@/composables'
import { processMenuData, processTopMenus } from '@/utils/menu-utils'

const route = useRoute()
const appStore = useAppStore()
const permissionStore = usePermissionStore()

const { activeKey: baseActiveKey, handleMenuSelect: baseHandleMenuSelect, findMenuIdByPath } = useMenu()

// Get side menu options based on selected top menu
const sideMenuOptions = computed(() => {
  const menus = permissionStore.menus || []

  if (!menus.length || !permissionStore.menuDataLoaded) {
    return []
  }

  const topMenus = processTopMenus(menus)
  let activeTopMenu = findTopMenuByPath(topMenus, route.path)

  // Fallback: path prefix match
  if (!activeTopMenu) {
    const pathSegments = route.path.split('/').filter(Boolean)
    if (pathSegments.length > 0) {
      const firstSegment = `/${pathSegments[0]}`
      activeTopMenu = topMenus.find((menu) => {
        if (menu.path && menu.path.startsWith(firstSegment)) {
          return true
        }
        if (menu.children && menu.children.length > 0) {
          return menu.children.some(child =>
            child.path && child.path.startsWith(firstSegment),
          )
        }
        return false
      })
    }
  }

  // Fallback: use store selected ID
  if (!activeTopMenu && appStore.selectedTopMenuId) {
    activeTopMenu = topMenus.find(item => item.id === appStore.selectedTopMenuId)
  }

  if (activeTopMenu && appStore.selectedTopMenuId !== activeTopMenu.id) {
    appStore.setSelectedTopMenuId(activeTopMenu.id)
  }

  if (activeTopMenu && activeTopMenu.children) {
    return processMenuData(activeTopMenu.children)
  }
  return []
})

// Active key computed - override base to use sideMenuOptions
const activeKey = computed(() => {
  if (route.meta?.parentKey) {
    return route.meta.parentKey
  }

  let menuId = findMenuIdByPath(sideMenuOptions.value, route.path)

  if (!menuId) {
    const pathSegments = route.path.split('/').filter(Boolean)
    for (let i = pathSegments.length - 1; i > 0; i--) {
      const parentPath = `/${pathSegments.slice(0, i).join('/')}`
      menuId = findMenuIdByPath(sideMenuOptions.value, parentPath)
      if (menuId)
        break
    }
  }

  return menuId || route.path
})

const menu = ref(null)
watch(route, async () => {
  await nextTick()
  menu.value?.showOption()
})

function handleMenuSelect(key) {
  baseHandleMenuSelect(key)
}
</script>

<style>
/* 统一侧边菜单选中态（与 modern-side-menu 对齐） */
.side-menu {
  padding: 6px 0;
  background: transparent;
}

.side-menu .n-menu-item-content {
  position: relative;
  margin: 1px 6px;
  border-radius: var(--radius-md);
  transition:
    background-color var(--transition-fast),
    color var(--transition-fast);
  font-size: 13px;
  font-weight: 400;
  color: var(--text-secondary);
  min-height: 38px;
  padding: 0 12px !important;
}

.side-menu .n-menu-item-content:hover {
  color: var(--primary-500);
  background: var(--primary-50);
}

.side-menu .n-menu-item-content--selected {
  background: var(--primary-50) !important;
  color: var(--primary-500) !important;
  font-weight: 500;
}

.side-menu .n-menu-item-content--selected:hover {
  background: var(--primary-50) !important;
  color: var(--primary-500) !important;
}

.side-menu .n-menu-item-content--selected::after {
  content: '';
  position: absolute;
  left: 0;
  top: 20%;
  height: 60%;
  width: 3px;
  background: var(--primary-500);
  border-radius: 0 2px 2px 0;
}

.side-menu .n-menu-item-content__icon {
  font-size: 16px;
  margin-right: 8px !important;
  color: var(--text-tertiary);
  opacity: 0.8;
}

.side-menu .n-menu-item-content:hover .n-menu-item-content__icon {
  color: var(--primary-500);
  opacity: 1;
}

.side-menu .n-menu-item-content--selected .n-menu-item-content__icon {
  color: var(--primary-500) !important;
  opacity: 1;
}

.side-menu.n-menu--collapsed .n-menu-item-content {
  justify-content: center;
  padding: 0 !important;
  width: 38px;
  height: 38px;
  margin: 2px auto;
}

.side-menu.n-menu--collapsed .n-menu-item-content__icon {
  margin-right: 0 !important;
}

.side-menu.n-menu--collapsed .n-menu-item-content-header,
.side-menu.n-menu--collapsed .n-menu-item-content__arrow {
  display: none !important;
}

.side-menu.n-menu--collapsed .n-menu-item-content--selected::after {
  display: none;
}
</style>
