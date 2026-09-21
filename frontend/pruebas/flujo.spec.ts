import { test, expect } from "@playwright/test";
test("profesor, QR, alumno móvil, WebSocket, respuestas y cierre", async ({
  page,
  browser,
  request,
}) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { name: /Una clase/ })).toBeVisible();
  await page.screenshot({
    path: "../docs/evidencias/inicio.png",
    fullPage: true,
  });
  await page.getByRole("button", { name: /Crear crucigrama/ }).click();
  await page
    .getByLabel("Nombre de la partida", { exact: true })
    .fill("Docker en el aula");
  await page
    .getByLabel("Materia", { exact: true })
    .fill("Ingeniería del Software");
  await page.getByLabel("Nombre del profesor", { exact: true }).fill("Eduardo");
  await page
    .getByLabel("Correo del profesor", { exact: true })
    .fill("profesor@example.test");
  await page.getByLabel("Cantidad de palabras").selectOption("5");
  await page.getByRole("button", { name: "Probar ejemplo Docker" }).click();
  await page.getByRole("button", { name: "Generar tablero" }).click();
  await expect(
    page.getByText("Tablero validado:", { exact: false }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Crear sala y obtener QR →" }).click();
  await expect(page.getByText("La sala está lista.")).toBeVisible();
  const s = await page.evaluate(() =>
    JSON.parse(sessionStorage.getItem("crossed-profesor")!),
  );
  const api = process.env.BACKEND_URL || "http://localhost:8080";
  const panel = await (
    await request.get(api + "/api/partidas/" + s.idPartida, {
      headers: { "X-Token": s.token },
    })
  ).json();
  const contexto = await browser.newContext({
    viewport: { width: 390, height: 844 },
  });
  const alumno = await contexto.newPage();
  let eventos = 0;
  alumno.on("websocket", (ws) =>
    ws.on("framereceived", (e) => {
      if (String(e.payload).includes("PARTIDA_INICIADA")) eventos++;
    }),
  );
  await alumno.goto("/?codigo=" + s.codigoVisible);
  await alumno
    .getByLabel("Tu nombre", { exact: true })
    .fill("Estudiante de prueba");
  await alumno
    .getByLabel("Tu correo", { exact: true })
    .fill("alumno@example.test");
  await alumno.getByRole("button", { name: "Entrar al desafío →" }).click();
  await expect(alumno.getByText(/Ya somos 1 participantes/)).toBeVisible();
  await page.screenshot({
    path: "../docs/evidencias/profesor.png",
    fullPage: true,
  });
  await page.getByRole("button", { name: "Iniciar partida ▶" }).click();
  await expect(alumno.getByText("Comprobar →")).toBeVisible({ timeout: 15000 });
  await expect(alumno.locator(".cuenta")).toBeHidden({ timeout: 15000 });
  expect(eventos).toBeGreaterThan(0);
  const ancho = await alumno.evaluate(() => ({
    pantalla: innerWidth,
    documento: document.documentElement.scrollWidth,
  }));
  expect(ancho.documento).toBeLessThanOrEqual(ancho.pantalla);
  await alumno.screenshot({
    path: "../docs/evidencias/alumno-movil.png",
    fullPage: true,
  });
  for (const palabra of panel.crucigrama.palabras) {
    await alumno
      .locator(".pista")
      .filter({
        has: alumno.locator("b", {
          hasText: new RegExp("^" + palabra.idPalabra + "$"),
        }),
      })
      .evaluate((b: HTMLElement) => b.click());
    await alumno
      .getByLabel("Tu respuesta", { exact: true })
      .fill(palabra.palabra);
    await alumno.getByRole("button", { name: "Comprobar →" }).click();
    if (palabra !== panel.crucigrama.palabras.at(-1))
      await expect(
        alumno.getByText("¡Correcto! Palabra completada."),
      ).toBeVisible();
  }
  await expect(
    alumno.getByRole("heading", { name: "¡Desafío finalizado!" }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "¡Crucigrama finalizado!" }),
  ).toBeVisible({ timeout: 10000 });
  await page.screenshot({
    path: "../docs/evidencias/resultado.png",
    fullPage: true,
  });
  expect(
    (
      await (
        await request.get(api + "/api/partidas/" + s.idPartida + "/ranking", {
          headers: { "X-Token": s.token },
        })
      ).json()
    )[0].puntaje,
  ).toBe(100);
  await request.delete(api + "/api/partidas/" + s.idPartida, {
    headers: { "X-Token": s.token },
  });
  await contexto.close();
});
