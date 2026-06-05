<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">产业园销售认购</div>
      <el-menu :default-active="route.path" router background-color="#1f2d3d" text-color="#c0c4cc" active-text-color="#ffffff">
        <el-menu-item index="/dashboard"><el-icon><DataLine /></el-icon><span>销售看板</span></el-menu-item>
        <el-menu-item index="/factory-units"><el-icon><OfficeBuilding /></el-icon><span>房源管理</span></el-menu-item>
        <el-menu-item index="/customers"><el-icon><User /></el-icon><span>客户管理</span></el-menu-item>
        <el-menu-item index="/subscriptions"><el-icon><Tickets /></el-icon><span>认购管理</span></el-menu-item>
        <el-menu-item index="/contracts"><el-icon><Document /></el-icon><span>合同管理</span></el-menu-item>
        <el-menu-item index="/approvals">
          <el-icon><Stamp /></el-icon><span>审批待办</span>
          <el-badge v-if="todoCount > 0" :value="todoCount" class="todo-badge" />
        </el-menu-item>
        <el-sub-menu index="system" v-if="isAdmin">
          <template #title><el-icon><Setting /></el-icon><span>系统设置</span></template>
          <el-menu-item index="/system/config">参数配置</el-menu-item>
          <el-menu-item index="/system/users">用户管理</el-menu-item>
          <el-menu-item index="/system/roles">角色管理</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="title">{{ route.meta.title || '' }}</div>
        <el-dropdown @command="onCommand">
          <span class="user">
            <el-icon><Avatar /></el-icon>
            {{ auth.realName || auth.username }}
            <el-tag size="small" v-for="r in auth.roles" :key="r" class="role-tag">{{ roleName(r) }}</el-tag>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { approvalApi } from '../api'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const todoCount = ref(0)
const isAdmin = computed(() => auth.roles.includes('ADMIN'))

const roleMap = {
  ADMIN: '管理员', SALES: '销售', SALES_MANAGER: '销售经理', FINANCE: '财务', LEGAL: '法务', GM: '总经理'
}
const roleName = (r) => roleMap[r] || r

const loadTodo = async () => {
  try {
    const res = await approvalApi.todo()
    todoCount.value = (res.data || []).length
  } catch (e) {
    todoCount.value = 0
  }
}

const onCommand = (cmd) => {
  if (cmd === 'logout') {
    auth.logout()
    router.push('/login')
  }
}

onMounted(loadTodo)
</script>

<style scoped>
.layout { height: 100vh; }
.aside { background: #1f2d3d; }
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  background: #16222e;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}
.title { font-size: 18px; font-weight: 600; color: #1f2d3d; }
.user { display: flex; align-items: center; gap: 6px; cursor: pointer; color: #1f2d3d; }
.role-tag { margin-left: 4px; }
.main { background: #f0f2f5; padding: 18px; }
.todo-badge { margin-left: 6px; }
.el-menu { border-right: none; }
</style>
