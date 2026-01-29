/// <reference types="vitest" />
import { defineConfig } from 'vite';
import type { UserConfig as VitestUserConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

const vitestConfig: VitestUserConfig['test'] = {
  globals: true,
  environment: 'jsdom',
  setupFiles: './vitest.setup.ts',
};

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: vitestConfig,
});
