/** @type {import('tailwindcss').Config} */
export default {
  content: ['./src/**/*.{astro,html,js,jsx,md,mdx,svelte,ts,tsx,vue}'],
  theme: {
    extend: {
      colors: {
        // SmilePile Brand Colors (from app)
        'smilepile-yellow': '#FFBF00',
        'smilepile-green': '#4CAF50',
        'smilepile-blue': '#2196F3',
        'smilepile-orange': '#FF6600',
        'smilepile-pink': '#E86082',

        // Category/Pile Colors
        'pile-red': '#FF6B6B',
        'pile-teal': '#4ECDC4',
        'pile-aqua': '#45B7D1',
        'pile-purple': '#DDA0DD',
        'pile-yellow': '#FFEAA7',

        // UI Colors (keeping primary as blue for consistency)
        primary: {
          DEFAULT: '#2196F3',
          50: '#E3F2FD',
          100: '#BBDEFB',
          500: '#2196F3',
          600: '#1E88E5',
          700: '#1976D2',
        },
      },
      fontFamily: {
        sans: ['-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
        display: ['Nunito', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
      },
      fontSize: {
        'xs': '0.875rem',    // 14px
        'sm': '1rem',        // 16px
        'base': '1rem',      // 16px
        'lg': '1.125rem',    // 18px
        'xl': '1.375rem',    // 22px
        '2xl': '1.75rem',    // 28px
        '3xl': '2.25rem',    // 36px
        '4xl': '3rem',       // 48px
      },
      lineHeight: {
        'tight': '1.1',
        'snug': '1.2',
        'normal': '1.3',
        'relaxed': '1.5',
        'loose': '1.6',
      },
      maxWidth: {
        'content': '65ch',
      },
    },
  },
  plugins: [],
}

