import { defineStore } from 'pinia'
import { ssoApi } from '../api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    username: localStorage.getItem('username') || '',
    realName: localStorage.getItem('realName') || '',
    roles: JSON.parse(localStorage.getItem('roles') || '[]')
  }),
  getters: {
    isLoggedIn: (state) => !!state.token
  },
  actions: {
    setLogin(data) {
      this.token = data.token
      this.username = data.username
      this.realName = data.realName
      this.roles = data.roles || []
      localStorage.setItem('token', this.token)
      localStorage.setItem('username', this.username)
      localStorage.setItem('realName', this.realName || '')
      localStorage.setItem('roles', JSON.stringify(this.roles))
    },
    // OA 单点登录自动登录：用 OA 的 sso_token 换取本系统登录态
    async ssoAutoLogin(token) {
      const res = await ssoApi.autoLogin({ token })
      this.setLogin(res.data)
    },
    logout() {
      this.token = ''
      this.username = ''
      this.realName = ''
      this.roles = []
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('realName')
      localStorage.removeItem('roles')
    },
    hasRole(role) {
      return this.roles.includes(role)
    }
  }
})
