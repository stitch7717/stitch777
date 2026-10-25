<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon :size="22" color="#fff"><FirstAidKit /></el-icon>
        <span>医疗后台管理</span>
      </div>
      <el-scrollbar>
        <el-menu
          :default-active="activeMenu"
          background-color="#001529"
          text-color="rgba(255,255,255,0.68)"
          active-text-color="#ffffff"
          router
          class="menu"
        >
          <template v-for="item in menuStore.menus" :key="item.id">
            <!-- 目录：有子节点渲染子菜单，无子节点渲染自身 -->
            <el-sub-menu
              v-if="item.menuType === 1 && item.children && item.children.length"
              :index="item.path || String(item.id)"
            >
              <template #title>
                <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
                <span>{{ item.menuName }}</span>
              </template>
              <el-menu-item
                v-for="child in item.children"
                :key="child.id"
                :index="resolvePath(item.path, child.path)"
              >
                <el-icon v-if="child.icon"><component :is="child.icon" /></el-icon>
                <span>{{ child.menuName }}</span>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="resolvePath('', item.path)">
              <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
              <span>{{ item.menuName }}</span>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container>
      <el-header class="header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/overview' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item v-if="route.meta?.title">{{ route.meta.title }}</el-breadcrumb-item>
        </el-breadcrumb>
        <div class="header-right">
          <el-badge :value="unreadCount" :hidden="!unreadCount" class="bell-badge">
            <el-icon :size="18" class="bell" @click="goMessage"><Bell /></el-icon>
          </el-badge>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="28" class="avatar">
                {{ (userStore.userInfo?.realName || 'A').charAt(0) }}
              </el-avatar>
              <span class="name">{{ userStore.userInfo?.realName }}</span>
              <el-tag size="small" type="info" effect="plain">
                {{ userStore.userInfo?.roleName }}
              </el-tag>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useMenuStore } from '@/store/menu'
import { getUnreadCount } from '@/api/system'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const menuStore = useMenuStore()
const unreadCount = ref(0)

const activeMenu = computed(() => route.path)

onMounted(() => {
  if (!menuStore.loaded) {
    menuStore.loadMenus().catch(() => ElMessage.error('菜单加载失败'))
  }
  getUnreadCount().then((res) => (unreadCount.value = res.data.count)).catch(() => {})
})

function goMessage() {
  router.push('/message/center')
}

function resolvePath(parentPath, childPath) {
  if (!childPath) return ''
  if (childPath.startsWith('/')) return childPath
  return parentPath ? `/${parentPath}/${childPath}`.replace(/\/+/g, '/') : `/${childPath}`
}

async function handleCommand(command) {
  if (command === 'logout') {
    await userStore.logout()
    menuStore.reset()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style scoped lang="scss">
.layout {
  height: 100%;
}

.aside {
  background: #001529;
  display: flex;
  flex-direction: column;

  .logo {
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    color: #fff;
    font-size: 16px;
    font-weight: 600;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
    flex-shrink: 0;
  }

  .menu {
    border-right: none;

    :deep(.el-menu-item.is-active) {
      background-color: #1f6feb;
    }
  }
}

.header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;

  .avatar {
    background: #1f6feb;
    color: #fff;
    font-size: 14px;
  }

  .name {
    font-size: 14px;
    color: #303133;
  }
}

.bell-badge {
  margin-right: 18px;
  cursor: pointer;

  .bell {
    color: #606266;
  }
}

.main {
  background: #f5f7fa;
  padding: 16px;
}
</style>
