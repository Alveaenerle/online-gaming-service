export default {
  preset: "ts-jest",
  testEnvironment: "jest-environment-jsdom",
  setupFilesAfterEnv: ["<rootDir>/src/setupTests.ts"],
  moduleNameMapper: {
    "\\.(css|less|sass|scss)$": "identity-obj-proxy",
    "\\.(gif|ttf|eot|svg|png)$": "<rootDir>/src/__mocks__/fileMock.js",
  },
  // Dodaj tę sekcję poniżej:
  transform: {
    "^.+\\.tsx?$": [
      "ts-jest",
      {
        diagnostics: {
          ignoreCodes: [1343], // Ignoruje błąd TS1343 dotyczący import.meta
        },
        astTransformers: {
          before: [
            {
              path: "ts-jest-mock-import-meta", // wymaga instalacji: npm install --save-dev ts-jest-mock-import-meta
              options: {
                metaObjectKey: "env",
                compileMiddlewares: ["ViteEnv"],
              },
            },
          ],
        },
      },
    ],
  },
};
