import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// En dev : `kubectl -n istio-system port-forward svc/istio-ingressgateway 8443:443`
// puis `npm run dev` ; /api est relayé vers la gateway.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': { target: 'https://localhost:8443', changeOrigin: true, secure: false },
    },
  },
})
