import request from './request'

export const authApi = {
  login: (data) => request.post('/auth/login', data),
  me: () => request.get('/auth/me')
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
  remove: (flowId) => request.delete(`/approvals/flows/${flowId}`)
}

export const configApi = {
  all: () => request.get('/system/config'),
  get: (key) => request.get(`/system/config/${key}`),
  batchUpdate: (updates) => request.put('/system/config/batch', updates)
}

export const systemUserApi = {
  list: (keyword) => request.get('/system/users', { params: { keyword } }),
  get: (id) => request.get(`/system/users/${id}`),
  create: (data) => request.post('/system/users', data),
  update: (id, data) => request.put(`/system/users/${id}`, data),
  remove: (id) => request.delete(`/system/users/${id}`),
  setEnabled: (id, enabled) => request.post(`/system/users/${id}/enabled`, { enabled }),
  resetPassword: (id, password) => request.post(`/system/users/${id}/reset-password`, { password })
}

export const systemRoleApi = {
  list: () => request.get('/system/roles'),
  create: (data) => request.post('/system/roles', data),
  update: (id, data) => request.put(`/system/roles/${id}`, data),
  remove: (id) => request.delete(`/system/roles/${id}`)
}

export const paymentApi = {
  listByContract: (contractId) => request.get('/payments', { params: { contractId } }),
  create: (data) => request.post('/payments', data)
}

export const dashboardApi = {
  summary: () => request.get('/dashboard/summary')
}
