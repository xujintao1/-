<template>
  <div class="login-container">
    <!-- 左侧 Logo 背景 -->
    <div class="logo-background">
      <div class="logo-content">
        <svg class="logo-icon" viewBox="0 0 400 400" preserveAspectRatio="xMidYMid meet">
          <g transform="translate(0,400) scale(0.1,-0.1)" fill="#1E5BA8" stroke="none">
            <path d="M1566 3782 c10 -10 -18 3 684 -307 294 -131 713 -316 930 -412 217
            -96 399 -177 405 -180 5 -3 -195 200 -445 451 l-455 456 -564 0 c-311 0 -561
            -4 -555 -8z"/>
            <path d="M1320 3729 c0 -3 -77 -349 -170 -769 -94 -420 -169 -764 -167 -766 1
            -2 214 208 472 466 l470 470 -303 302 c-166 167 -302 300 -302 297z"/>
            <path d="M715 3200 l-479 -479 274 -278 c150 -153 288 -293 306 -312 l32 -33
            10 48 c6 27 86 382 176 788 91 407 164 741 163 742 -2 2 -219 -212 -482 -476z"/>
            <path d="M1740 3473 c107 -109 610 -615 1118 -1124 l922 -924 -2 629 -3 628
            -360 160 c-198 88 -691 306 -1095 485 -404 179 -744 330 -755 336 -11 5 68
            -80 175 -190z"/>
            <path d="M213 2385 c4 -109 7 -392 7 -630 l0 -432 148 -65 c81 -37 448 -200
            817 -363 369 -163 801 -355 961 -426 160 -71 295 -129 300 -129 8 0 -2215
            2232 -2230 2239 -6 2 -7 -76 -3 -194z"/>
            <path d="M2645 1449 c-264 -264 -471 -479 -460 -476 11 3 358 86 770 183 413
            98 752 180 755 181 2 2 -128 136 -290 298 l-295 295 -480 -481z"/>
            <path d="M2890 1032 c-426 -102 -777 -187 -780 -189 -2 -2 132 -140 298 -306
            l302 -302 492 493 c271 270 486 492 478 491 -8 -1 -364 -85 -790 -187z"/>
            <path d="M740 775 c184 -181 387 -380 451 -442 l115 -113 563 2 562 3 -328
            142 c-311 135 -1049 455 -1473 640 -107 46 -202 87 -210 91 -8 3 136 -142 320
            -323z"/>
          </g>
        </svg>
        <div class="logo-text">
          <div class="logo-text-cn">智造产业园</div>
          <div class="logo-text-en">SALES SUBSCRIPTION</div>
        </div>
      </div>
    </div>

    <!-- 右侧登录表单 -->
    <div class="login-box">
      <div class="login-header">
        <h1 class="title">销售认购系统</h1>
        <p class="subtitle">产业园厂房销售认购管理平台</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" class="login-form" @keyup.enter="onLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" class="login-btn" @click="onLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <p>演示账号：admin/admin123 · manager/manager123</p>
        <p>finance/finance123 · legal/legal123 · sales/sales123</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { authApi } from '../api'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'admin123' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const onLogin = async () => {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await authApi.login(form)
      auth.setLogin(res.data)
      ElMessage.success('登录成功')
      router.push('/')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 10%;
  background: #ffffff;
  position: relative;
  overflow: hidden;
}

.logo-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 55%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.logo-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.logo-icon {
  width: 80px;
  height: 80px;
  flex-shrink: 0;
}

.logo-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.logo-text-cn {
  font-size: 36px;
  font-weight: 700;
  color: #1e5ba8;
  letter-spacing: 4px;
  line-height: 1.2;
}

.logo-text-en {
  font-size: 14px;
  font-weight: 500;
  color: #1e5ba8;
  letter-spacing: 2px;
  opacity: 0.8;
}

.login-box {
  position: relative;
  z-index: 10;
  width: 420px;
  min-height: 450px;
  padding: 60px 45px;
  background: rgba(255, 255, 255, 0.98);
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(37, 99, 235, 0.15);
  backdrop-filter: blur(10px);
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-header .title {
  font-size: 26px;
  font-weight: 700;
  color: #333;
  margin: 0 0 8px 0;
}

.login-header .subtitle {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.login-form .login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  border-radius: 8px;
  background: #1e5ba8;
  border: none;
}

.login-form .login-btn:hover {
  background: #174a8c;
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  line-height: 1.8;
}

.login-footer p {
  color: #999;
  font-size: 12px;
  margin: 0;
}

@media (max-width: 768px) {
  .login-container {
    flex-direction: column;
    justify-content: flex-start;
    align-items: center;
    padding: 30px 16px 0;
    min-height: 100vh;
  }

  .logo-background {
    position: static;
    width: 100%;
    height: auto;
    padding: 20px 0 30px;
  }

  .logo-content {
    gap: 12px;
  }

  .logo-icon {
    width: 56px;
    height: 56px;
  }

  .logo-text-cn {
    font-size: 22px;
    letter-spacing: 2px;
  }

  .logo-text-en {
    font-size: 11px;
    letter-spacing: 1px;
  }

  .login-box {
    width: 100%;
    max-width: 100%;
    min-height: auto;
    padding: 32px 24px;
    border-radius: 12px;
    box-shadow: 0 4px 20px rgba(37, 99, 235, 0.1);
  }
}
</style>
