import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import LinksPage from "../pages/LinksPage";

const link = {
  code: "abc",
  shortUrl: "http://localhost:8080/abc",
  url: "https://example.com/docs",
  expiresAt: null,
  createdAt: "2026-11-15T00:00:00Z",
  screening: {
    safe: true,
    reason: "No suspicious pattern found",
    title: "example.com",
    summary: "Rule check found nothing unusual.",
    source: "rules",
  },
};

function jsonResponse(body, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    headers: { get: () => null },
    text: async () => (body == null ? "" : JSON.stringify(body)),
  };
}

describe("LinksPage", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("shows an empty list", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse([])));
    render(
      <MemoryRouter>
        <LinksPage />
      </MemoryRouter>,
    );
    expect(await screen.findByText("No links yet. Add one above.")).toBeInTheDocument();
  });

  it("lists a short link with a copy button", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse([link])));
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, "clipboard", {
      configurable: true,
      value: { writeText },
    });

    render(
      <MemoryRouter>
        <LinksPage />
      </MemoryRouter>,
    );

    expect(await screen.findByText("http://localhost:8080/abc")).toBeInTheDocument();
    expect(screen.getByText("Safe")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Copy" }));
    expect(writeText).toHaveBeenCalledWith("http://localhost:8080/abc");
    expect(screen.getByRole("button", { name: "Copied" })).toBeInTheDocument();
  });
});
