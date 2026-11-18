// Client de l'API REST exposée par pantry-service derrière la gateway Istio.
async function request(path, options = {}) {
  const res = await fetch(path, {
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    ...options,
  })
  if (!res.ok) {
    let detail = `Erreur ${res.status}`
    try {
      const problem = await res.json()
      detail = problem.detail || problem.title || detail
    } catch { /* corps non JSON */ }
    throw new Error(detail)
  }
  return res.status === 204 ? null : res.json()
}

export const api = {
  items: () => request('/api/items'),
  stats: () => request('/api/items/stats'),
  suggestions: (limit = 6) => request(`/api/suggestions?limit=${limit}`),
  addItem: (item) => request('/api/items', { method: 'POST', body: JSON.stringify(item) }),
  removeItem: (id) => request(`/api/items/${id}`, { method: 'DELETE' }),
}
