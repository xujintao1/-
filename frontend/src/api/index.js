import request from './request'

export const authApi = {
  login: (data) => request.post('/auth/login', data),
  me: () => request.get('/auth/me')
}

// SSO 单点登录（对接外部 OA 系统），参照 hr- 仓库
export const ssoApi = {
  login: (data) => request.post('/sso/login', data),
  autoLogin: (data) => request.post('/sso/auto-login', data)
}

export const factoryUnitApi = {
  page: (params) => request.get('/factory-units', { params }),
  create: (data) => request.post('/factory-units', data),
  update: (id, data) => request.put(`/factory-units/${id}`, data),
  remove: (id) => request.delete(`/factory-units/${id}`)
}

export const customerApi = {
  page: (params) => request.get('/customers', { params }),
  all: () => request.get('/customers/all'),
  create: (data) => request.post('/customers', data),
  update: (id, data) => request.put(`/customers/${id}`, data),
  remove: (id) => request.delete(`/customers/${id}`)
}

export const subscriptionApi = {
  page: (params) => request.get('/subscriptions', { params }),
  create: (data) => request.post('/subscriptions', data),
  cancel: (id) => request.post(`/subscriptions/${id}/cancel`)
}

export const contractApi = {
  page: (params) => request.get('/contracts', { params }),
  get: (id) => request.get(`/contracts/${id}`),
  approvalTasks: (id) => request.get(`/contracts/${id}/approval-tasks`),
  create: (data) => request.post('/contracts', data),
  submit: (id) => request.post(`/contracts/${id}/submit`)
}

export const approvalApi = {
  todo: () => request.get('/approvals/todo'),
  preview: (bizType = 'CONTRACT') => request.get('/approvals/preview', { params: { bizType } }),
  flowDetail: (flowId) => request.get(`/approvals/flows/${flowId}`),
  approve: (taskId, data) => request.post(`/approvals/${taskId}/approve`, data),
  reject: (taskId, data) => request.post(`/approvals/${taskId}/reject`, data),
  withdraw: (flowId, data) => request.post(`/approvals/flows/${flowId}/withdraw`, data),
  resubmit: (flowId) => request.post(`/approvals/flows/${flowId}/resubmit`),
  remove: (flowId) => request.delete(`/approvals/flows/${flowId}`),
  resync: (flowId) => request.post(`/approvals/flows/${flowId}/resync`)
}

export const configApi = {
  all: () => request.get('/system/config'),
  get: (key) => request.get(`/system/config/${key}`),
  batchUpdate: (updates) => request.put('/system/config/batch', updates)
}

// 用户数据同步自 OA 系统，本系统仅做角色/启用授权（参照 hr- 仓库）
export const systemUserApi = {
  list: (params) => request.get('/system/users', { params }),
  get: (id) => request.get(`/system/users/${id}`),
  assignRole: (data) => request.post('/system/users/assign-role', data)
}

export const systemRoleApi = {
  list: () => request.get('/system/roles'),
  create: (data) => request.post('/system/roles', data),
  update: (id, data) => request.put(`/system/roles/${id}`, data),
  remove: (id) => request.delete(`/system/roles/${id}`)
}

// 部门数据同步自 OA 系统，本系统只读展示（参照 hr- 仓库）
export const systemDeptApi = {
  tree: () => request.get('/system/dept/tree'),
  list: () => request.get('/system/dept/list')
}

export const systemMenuApi = {
  list: () => request.get('/system/menu/list'),
  create: (data) => request.post('/system/menu', data),
  update: (data) => request.put('/system/menu', data),
  remove: (id) => request.delete(`/system/menu/${id}`)
}

export const operLogApi = {
  page: (params) => request.get('/system/oper-log/page', { params }),
  clear: () => request.delete('/system/oper-log/clear')
}

export const paymentApi = {
  listByContract: (contractId) => request.get('/payments', { params: { contractId } }),
  create: (data) => request.post('/payments', data)
}

export const dashboardApi = {
  summary: () => request.get('/dashboard/summary')
}
