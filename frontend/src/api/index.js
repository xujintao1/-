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
  approve: (taskId, data) => request.post(`/approvals/${taskId}/approve`, data),
  reject: (taskId, data) => request.post(`/approvals/${taskId}/reject`, data)
}

export const paymentApi = {
  listByContract: (contractId) => request.get('/payments', { params: { contractId } }),
  create: (data) => request.post('/payments', data)
}

export const dashboardApi = {
  summary: () => request.get('/dashboard/summary')
}
