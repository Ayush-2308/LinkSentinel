import { render, screen } from "@testing-library/react";
import VerdictBadge from "../components/VerdictBadge";

describe("VerdictBadge", () => {
  it("shows a safe verdict", () => {
    render(<VerdictBadge screening={{ safe: true, reason: "ok", title: "Docs", summary: "Fine", source: "rules" }} />);
    expect(screen.getByText("Safe")).toBeInTheDocument();
  });

  it("shows an unsafe verdict", () => {
    render(
      <VerdictBadge
        screening={{ safe: false, reason: "phishing", title: "Login", summary: "Asks for a password", source: "llm" }}
      />,
    );
    expect(screen.getByText("Unsafe")).toBeInTheDocument();
  });
});
