import {defineConfig} from "vite"
import react from "@vitejs/plugin-react"
import path from "path"

// Manual chunks keep the main bundle smaller so we avoid the 500 kB warning.
export default defineConfig({
  plugins: [react()],
    resolve: {
        alias: {
            "@": path.resolve(__dirname, "./"),
        },
    },
  server: {
    proxy: {
      "/api": "http://localhost:8080",
    },
  },
  build: {
    chunkSizeWarningLimit: 900,
    rollupOptions: {
      output: {
        manualChunks: {
          "vendor-react": ["react", "react-dom", "react-router", "react-router-dom"],
          "vendor-mui": ["@mui/material", "@mui/icons-material", "@emotion/react", "@emotion/styled"],
          "vendor-utils": ["dexie"],
        },
      },
    },
  },
})
