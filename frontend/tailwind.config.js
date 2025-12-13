/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        bg: "#0c0b10",
        purpleStart: "#6C2AFF",
        purpleEnd: "#A855F7",
      },
      boxShadow: {
        neon: "0 6px 30px rgba(108,42,255,0.18), 0 0 60px rgba(168,85,247,0.06)",
      },
    },
  },
  plugins: [],
};
