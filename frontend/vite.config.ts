/// <reference types="vitest/config" />
import {defineConfig} from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      // Während der Dev-Session fragt das Frontend `http://localhost:5173/api/...`
      // an, Vite reicht das transparent an Spring Boot auf 8080 durch — keine
      // CORS-Sorgen im Dev-Modus, in Prod läuft alles über denselben Origin.
      '/api': 'http://localhost:8080',
    },
  },
    test: {
        globals: true,
        environment: 'happy-dom',
        setupFiles: ['./src/test/setup.ts'],
        coverage: {
            provider: 'v8',
            reporter: ['text', 'html', 'lcov'],
            exclude: [
                '**/*.config.{ts,js}',
                '**/node_modules/**',
                '**/dist/**',
                'src/main.tsx',
                'src/vite-env.d.ts',
            ],
        },
    },
});
