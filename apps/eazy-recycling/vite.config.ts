import react from '@vitejs/plugin-react';
import { visualizer } from 'rollup-plugin-visualizer';
import { type PluginOption } from 'vite';
import svgr from 'vite-plugin-svgr';
import tsconfigPaths from 'vite-tsconfig-paths';
import { defineConfig } from 'vitest/config';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), svgr(), tsconfigPaths(), visualizer() as PluginOption],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './vitest.setup.ts',
    // speed up since tests don't rely on css
    // https://github.com/vitest-dev/vitest/blob/main/examples/react-testing-lib/vite.config.ts#L14-L16
    css: false,
  },
  resolve: {
    // Ensure SVG imports are properly resolved
    extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.svg'],
  },
  build: {
    rollupOptions: {
      input: {
        main: './index.html',
        mobile: './mobile.html',
      },
      output: {
        manualChunks: (id: string) => {
          // Common chunks for both desktop and mobile
          if (id.includes('node_modules/@supabase/supabase-js')) {
            return 'supabase';
          }
          if (id.includes('node_modules/date-fns')) {
            return 'date-fns';
          }

          // Separate chunks for shared components
          if (
            id.includes('/components/auth/') ||
            id.includes('/components/common/')
          ) {
            return 'shared-components';
          }

          // Separate chunks for desktop and mobile
          if (id.includes('/src/desktop/')) {
            return 'desktop-app';
          }
          if (id.includes('/src/mobile/')) {
            return 'mobile-app';
          }
          return undefined;
        },
      },
    },
  },
});
