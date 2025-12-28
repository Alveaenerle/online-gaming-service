import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    proxy: {
      '/api/auth': {
        target: 'http://authorization:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/auth/, ''),
      },
      '/api/menu': {
        target: 'http://menu:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/menu/, ''),
      },
      '/api/social': {
        target: 'http://social:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/social/, ''),
      },
      '/api/makao': {
        target: 'http://makao:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/makao/, ''),
        ws: true,
      },
      '/api/ludo': {
        target: 'http://ludo:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/ludo/, ''),
        ws: true,
      },
    },
  },
})
