/** @type {import('tailwindcss').Config} */
import defaultTheme from 'tailwindcss/defaultTheme';

export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        "color-status-error-light": "var(--red-50)",
        "color-brand-light": "var(--blue-50)",
        "color-brand-light-hover": "var(--blue-100)",
        "color-brand-primary": "var(--blue-500)",
        "color-brand-dark": "var(--blue-700)",
        "color-border-primary": "var(--gray-200)",
        "color-border-hover": "var(--gray-300)",
        "color-status-error-primary": "var(--red-500)",
        "color-status-error-dark": "var(--red-700)",
        "color-status-success-light": "var(--green-50)",
        "color-status-success-primary": "var(--green-500)",
        "color-status-success-dark": "var(--green-700)",
        "color-status-warning-light": "var(--orange-50)",
        "color-status-warning-primary": "var(--orange-500)",
        "color-status-warning-dark": "var(--orange-700)",
        "color-status-info-light": "var(--blue-100)",
        "color-status-info-primary": "var(--blue-500)",
        "color-status-info-dark": "var(--blue-700)",
        "color-surface-primary": "var(--gray-0)",
        "color-surface-secondary": "var(--gray-50)",
        "color-surface-background": "var(--gray-50)",
        "color-surface-disabled": "var(--gray-100)",
        "color-surface-hover": "var(--gray-50)",
        "color-text-primary": "var(--gray-900)",
        "color-text-secondary": "var(--gray-700)",
        "color-text-invert-primary": "var(--gray-0)",
        "color-text-invert-secondary": "var(--gray-100)",
        "color-text-disabled": "var(--gray-400)"
      },
      borderRadius: {
        "radius-xs": "var(--number-4)",
        "radius-sm": "var(--number-6)",
        "radius-md": "var(--number-8)",
        "radius-lg": "var(--number-12)",
        "radius-xl": "var(--number-16)",
      },
      spacing: {
        // Tokens
        "spacing-xss": "var(--number-4)",
        "spacing-xs": "var(--number-8)",
        "spacing-sm": "var(--number-12)",
        "spacing-md": "var(--number-16)",
        "spacing-lg": "var(--number-24)",
        "spacing-xl": "var(--number-32)",
        "spacing-xll": "var(--number-40)"
      },
      backdropBlur: {
        xs: '2px',
      },
      border: {
        primary: 'var(--color-border-primary)',
        hover: 'var(--color-border-hover)',
      },
      customProperties: {
        'text-styles-font': 'Urbanist',
      },
      fontFamily: {
        body: ['Urbanist', ...defaultTheme.fontFamily.sans],
        heading: ['Urbanist', ...defaultTheme.fontFamily.sans],
        sans: ['Urbanist', ...defaultTheme.fontFamily.sans],
        inter: ['Inter', ...defaultTheme.fontFamily.sans],
      },
      fontSize: {
        "h1": ["2.5rem", { lineHeight: "3rem", fontWeight: "700" }],
        "h2": ["2rem", { lineHeight: "2.5rem", fontWeight: "700" }],
        "h3": ["1.5rem", { lineHeight: "2rem", fontWeight: "700" }],
        "h4": ["1.25rem", { lineHeight: "1.5rem", fontWeight: "700" }],
        "subtitle-1": ["1rem", { lineHeight: "1.5rem", fontWeight: "600" }],
        "subtitle-2": ["0.875rem", { lineHeight: "1.25rem", fontWeight: "600" }],
        "subtitle-tab": ["0.875rem", { lineHeight: "1.25rem", fontWeight: "600", textAlign: "center", fontFamily: "Inter" }],
        "body-1": ["1rem", { lineHeight: "1.5rem", fontWeight: "400" }],
        "body-2": ["0.875rem", { lineHeight: "1.25rem", fontWeight: "400" }],
        "button": ["0.875rem", { lineHeight: "1.25rem", fontWeight: "500" }],
        "caption-1": ["0.75rem", { lineHeight: "1rem", fontWeight: "400" }],
        "caption-2": ["0.75rem", { lineHeight: "1rem", fontWeight: "600" }],
      },
    },
  },
  plugins: [],
};
