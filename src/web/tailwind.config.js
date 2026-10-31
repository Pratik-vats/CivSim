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
        mundane: {
          bg: '#1a1815',
          card: '#24201b',
          border: '#3d362d',
          accent: '#c49a45',
          text: '#f2e8dc',
          muted: '#9c9183',
        },
        awakening: {
          bg: '#0c0f17',
          card: '#131926',
          border: '#243047',
          accent: '#06b6d4',
          purple: '#a855f7',
          danger: '#f43f5e',
          text: '#f1f5f9',
          muted: '#818cf8',
        }
      },
      fontFamily: {
        sans: ['Cinzel', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        mono: ['ui-monospace', 'SFMono-Regular', 'Menlo', 'monospace'],
      }
    },
  },
  plugins: [],
}

