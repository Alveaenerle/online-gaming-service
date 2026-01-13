import "@testing-library/jest-dom";

Object.defineProperty(global, "import", {
  value: {
    meta: {
      env: {
        VITE_API_URL: "/api/auth",
      },
    },
  },
});
