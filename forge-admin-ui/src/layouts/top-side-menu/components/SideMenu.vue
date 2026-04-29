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

.side-menu {
  /* 顶部+侧边菜单布局：统一选中色 */
  --top-side-menu-accent: #4B32FE;
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

.side-menu .n-menu-item-content__label {
  line-height: 1.4;
}

.side-menu .n-menu-item-content:hover {
  color: var(--top-side-menu-accent);
  background: var(--primary-50);
}

.side-menu .n-menu-item-content--selected {
  background: var(--primary-50) !important;
  color: var(--top-side-menu-accent) !important;
  font-weight: 500;
}

.side-menu .n-menu-item-content--selected:hover {
  background: var(--primary-50) !important;
  color: var(--top-side-menu-accent) !important;
}

.side-menu .n-menu-item-content--selected::after {
  content: '';
  position: absolute;
  left: 0;
  top: 20%;
  height: 60%;
  width: 3px;
  background: var(--top-side-menu-accent);
  border-radius: 0 2px 2px 0;
}

/* ===== 层级区分：二级(目录/子菜单标题) vs 三级(子项) ===== */
/* 二级菜单（Submenu 标题）：更像“分组标题” */
.side-menu .n-submenu > .n-menu-item-content {
  font-weight: 600;
  color: var(--text-primary);
}

.side-menu .n-submenu > .n-menu-item-content:hover {
  background: var(--primary-50);
  color: var(--top-side-menu-accent);
}

/* 二级展开时，标题有更明确的“正在浏览此分组”提示 */
.side-menu .n-submenu.n-submenu--expanded > .n-menu-item-content,
.side-menu .n-submenu.n-submenu--active > .n-menu-item-content {
  background: var(--primary-50); /* fallback */
  background: color-mix(in srgb, var(--primary-50) 60%, transparent);
}

/* 三级菜单（Submenu children）：更轻、更缩进，并提供“层级引导线” */
.side-menu .n-submenu-children {
  position: relative;
  margin: 2px 6px 6px;
  padding: 2px 0 2px 10px;
}

.side-menu .n-submenu-children::before {
  content: '';
  position: absolute;
  left: 14px;
  top: 4px;
  bottom: 4px;
  width: 1px;
  background: var(--border-light);
  border-radius: 1px;
}

.side-menu .n-submenu-children .n-menu-item-content {
  font-size: 12px;
  color: var(--text-tertiary);
  padding-left: 28px !important; /* 给圆点+引导线留空间 */
}

/* 三级项圆点：避免“看起来像同一层级” */
.side-menu .n-submenu-children .n-menu-item-content::before {
  content: '';
  position: absolute;
  left: 10px;
  top: 50%;
  width: 6px;
  height: 6px;
  border-radius: var(--radius-full);
  transform: translateY(-50%);
  background: var(--border-default);
}

.side-menu .n-submenu-children .n-menu-item-content:hover::before {
  background: var(--top-side-menu-accent);
}

.side-menu .n-submenu-children .n-menu-item-content--selected::before {
  background: var(--top-side-menu-accent);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--primary-50) 75%, transparent);
}

.side-menu .n-submenu-children .n-menu-item-content--selected::after {
  /* 子项沿用左侧指示条，但更“细”，避免和二级标题抢层级 */
  width: 2px;
  top: 22%;
  height: 56%;
}

.side-menu .n-menu-item-content__icon {
  font-size: 16px;
  margin-right: 8px !important;
  color: var(--text-tertiary);
  opacity: 0.8;
}

.side-menu .n-menu-item-content:hover .n-menu-item-content__icon {
  color: var(--top-side-menu-accent);
  opacity: 1;
}

.side-menu .n-menu-item-content--selected .n-menu-item-content__icon {
  color: var(--top-side-menu-accent) !important;
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
