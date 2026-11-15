import path from "node:path";
import { expect, test } from "@playwright/test";

const shots = path.join("..", "docs", "screenshots");

test("sign up, shorten a link, and see it in the list", async ({ page }) => {
  const email = `e2e-${Date.now()}@example.com`;
  await page.goto("/login");
  await page.screenshot({ path: path.join(shots, "login.png"), fullPage: true });
  await page.getByRole("button", { name: "Need an account? Create one" }).click();
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill("password123");
  await page.getByRole("button", { name: "Create account" }).click();
  await page.getByLabel("URL").fill("https://example.com/docs");
  await page.getByRole("button", { name: "Shorten" }).click();
  await expect(page.getByText("https://example.com/docs")).toBeVisible();
  await expect(page.getByRole("button", { name: "Copy" })).toBeVisible();
  await page.screenshot({ path: path.join(shots, "links.png"), fullPage: true });

  const shortUrl = (await page.locator("li p.break-all.text-accent").innerText()).trim();
  const code = shortUrl.substring(shortUrl.lastIndexOf("/") + 1);
  await page.request.get(shortUrl, { maxRedirects: 0 });
  await page.getByRole("link", { name: "Analytics" }).click();
  await expect(page).toHaveURL("/links/" + code);
  await expect(async () => {
    await page.reload();
    await expect(page.getByText("1 click")).toBeVisible();
  }).toPass({ timeout: 10000 });
  await page.screenshot({ path: path.join(shots, "analytics.png"), fullPage: true });
});
