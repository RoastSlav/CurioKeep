import {defineConfig} from "vitest/config"
import react from "@vitejs/plugin-react"
import path from "path"

// Vendor chunks keep the main bundle smaller so we avoid the 500 kB warning.
export default defineConfig({
  plugins: [react()],
    resolve: {
        alias: {
            "@": path.resolve(import.meta.dirname, "./src"),
        },
    },
  test: {
    environment: "jsdom",
    setupFiles: ["./src/test/setup.ts"],
  },
  server: {
    proxy: {
      "/api": "http://localhost:8080",
    },
  },
  build: {
    chunkSizeWarningLimit: 900,
    rolldownOptions: {
      output: {
        codeSplitting: {
          groups: [
            { name: "vendor-react", test: /node_modules[\\/](react|react-dom|react-router|react-router-dom)[\\/]/ },
          ],
        },
      },
    },
  },
})
