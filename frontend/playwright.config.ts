import { defineConfig } from "@playwright/test";
export default defineConfig({
  testDir: "./pruebas",
  timeout: 90000,
  use: {
    baseURL: process.env.FRONTEND_URL || "http://localhost:3000",
    headless: true,
    launchOptions: {
      ...(process.env.CHROME_PATH
        ? { executablePath: process.env.CHROME_PATH }
        : {}),
      args: ["--no-sandbox", "--disable-setuid-sandbox"],
    },
  },
  workers: 1,
  reporter: [
    ["list"],
    ["json", { outputFile: "test-results/resultados.json" }],
  ],
});
