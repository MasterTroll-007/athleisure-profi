/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#fff8ea',
          100: '#ffedc5',
          200: '#ffdc8b',
          300: '#ffc75a',
          400: '#ffb347',
          500: '#f29b2f',
          600: '#d97b1d',
          700: '#b55d19',
          800: '#92491d',
          900: '#783d1d',
        },
        neutral: {
          50: '#fafaf9',
          100: '#f5f5f4',
          200: '#e7e5e4',
          300: '#d6d3d1',
          400: '#a8a29e',
          500: '#78716c',
          600: '#57534e',
          700: '#44403c',
          800: '#292524',
          900: '#1c1917',
          950: '#0c0a09',
        },
        status: {
          available: '#e7f5e8',
          availableBorder: '#86c789',
          booked: '#e8e0f5',
          bookedBorder: '#a78bcc',
          pending: '#fff4e6',
          pendingBorder: '#f5a623',
          cancelled: '#f5e7e7',
          cancelledBorder: '#d6a5a5',
          mine: '#e0f0f5',
          mineBorder: '#7bb8cc',
        },
        dark: {
          bg: '#05040a',
          surface: '#111019',
          surfaceHover: '#1b1a25',
          border: '#2a2835',
        }
      },
      fontFamily: {
        heading: ['Outfit', 'sans-serif'],
        body: ['Outfit', 'sans-serif'],
        mono: ['Outfit', 'sans-serif'],
        display: ['Outfit', 'sans-serif'],
      },
      fontSize: {
        'display': ['2.25rem', { lineHeight: '2.5rem', fontWeight: '700' }],
      },
      borderRadius: {
        'sm': '8px',
        'md': '12px',
        'lg': '16px',
        'xl': '24px',
      },
      boxShadow: {
        'sm': '0 1px 2px 0 rgb(0 0 0 / 0.03)',
        'md': '0 4px 6px -1px rgb(0 0 0 / 0.05), 0 2px 4px -2px rgb(0 0 0 / 0.05)',
        'lg': '0 10px 15px -3px rgb(0 0 0 / 0.05), 0 4px 6px -4px rgb(0 0 0 / 0.05)',
        'glow': '0 0 20px rgb(224 93 82 / 0.15)',
      },
      screens: {
        'sm': '640px',
        'md': '768px',
        'lg': '1024px',
        'xl': '1280px',
      },
    },
  },
  plugins: [],
}
