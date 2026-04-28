<template>
  <n-drawer
    v-model:show="drawerVisible"
    :width="280"
    placement="left"
    :mask-closable="true"
  >
    <n-drawer-content closable title="菜单导航">
      <div class="drawer-menu-container">
        <!-- 搜索框 -->
        <div class="drawer-search">
          <n-input
            v-model:value="searchKeyword"
            placeholder="搜索菜单..."
            clearable
            size="small"
            round
          >
            <template #prefix>
              <i class="i-mdi-magnify" />
            </template>
          </n-input>
        </div>

        <!-- 菜单列表 -->
        <n-scrollbar class="drawer-menu-list">
          <n-menu
            class="drawer-side-menu"
            :options="filteredMenus"
            :value="activeKey"
            :indent="16"
            @update:value="handleMenuSelect"
          />
        </n-scrollbar>
      </div>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { usePermissionStore } from '@/store'
import { useMenu } from '@/composables'
import { processMenuData } from '@/utils/menu-utils'

const route = useRoute()
const permissionStore = usePermissionStore()

const { handleMenuSelect: baseHandleMenuSelect, findMenuIdByPath } = useMenu()

const drawerVisible = defineModel('show', { type: Boolean, default: false })
const emit = defineEmits(['select'])

const searchKeyword = ref('')

// Process menu data
const processedMenus = computed(() => {
  const menus = permissionStore.menus || []
  return processMenuData(menus)
})

// Filter menus by keyword
const filteredMenus = computed(() => {
  if (!searchKeyword.value.trim()) {
    return processedMenus.value
  }
  const keyword = searchKeyword.value.toLowerCase().trim()
  return filterMenus(processedMenus.value, keyword)
})

function filterMenus(items, keyword) {
  return items.reduce((acc, item) => {
    const titleMatch = (item.label || '').toLowerCase().includes(keyword)
    const filteredChildren = item.children ? filterMenus(item.children, keyword) : []

    if (titleMatch || filteredChildren.length > 0) {
      acc.push({
        ...item,
        children: filteredChildren.length > 0 ? filteredChildren : undefined,
      })
    }
    return acc
  }, [])
}

// Active menu key
const activeKey = computed(() => {
  if (route.meta?.parentKey) {
    return route.meta.parentKey
  }
  return findMenuIdByPath(route.path) || route.name
})

function handleMenuSelect(key) {
  baseHandleMenuSelect(key)
  emit('select')
  drawerVisible.value = false
}
</script>

<style scoped>
.drawer-menu-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.drawer-search {
  padding: 4px 0 12px;
}

.drawer-menu-list {
  flex: 1;
  overflow: hidden;
}

/* 让抽屉内菜单也有明显“正在使用”选中态 */
.drawer-side-menu {
  padding: 6px 0;
  background: transparent;
}

.drawer-side-menu :deep(.n-menu-item-content) {
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

.drawer-side-menu :deep(.n-menu-item-content:hover) {
  color: var(--primary-500);
  background: var(--primary-50);
}

.drawer-side-menu :deep(.n-menu-item-content--selected) {
  background: var(--primary-50) !important;
  color: var(--primary-500) !important;
  font-weight: 500;
}

.drawer-side-menu :deep(.n-menu-item-content--selected:hover) {
  background: var(--primary-50) !important;
  color: var(--primary-500) !important;
}

.drawer-side-menu :deep(.n-menu-item-content--selected::after) {
  content: '';
  position: absolute;
  left: 0;
  top: 20%;
  height: 60%;
  width: 3px;
  background: var(--primary-500);
  border-radius: 0 2px 2px 0;
}

.drawer-side-menu :deep(.n-menu-item-content__icon) {
  font-size: 16px;
  margin-right: 8px !important;
  color: var(--text-tertiary);
  opacity: 0.8;
}

.drawer-side-menu :deep(.n-menu-item-content:hover .n-menu-item-content__icon) {
  color: var(--primary-500);
  opacity: 1;
}

.drawer-side-menu :deep(.n-menu-item-content--selected .n-menu-item-content__icon) {
  color: var(--primary-500) !important;
  opacity: 1;
}
</style>
