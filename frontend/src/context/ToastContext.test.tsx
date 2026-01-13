import React from "react";
import {
  render,
  screen,
  act,
  waitFor,
  fireEvent,
} from "@testing-library/react";
import { ToastProvider, useToast } from "./ToastContext";

const TestConsumer = () => {
  const { showToast } = useToast();
  return (
    <div>
      <button onClick={() => showToast("Success message", "success")}>
        Show Success
      </button>
      <button onClick={() => showToast("Error message", "error")}>
        Show Error
      </button>
      <button onClick={() => showToast("Default message")}>Show Default</button>
    </div>
  );
};

describe("ToastContext", () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    act(() => {
      jest.runOnlyPendingTimers();
    });
    jest.useRealTimers();
  });

  it("should auto-remove toast after 2000ms", async () => {
    render(
      <ToastProvider>
        <TestConsumer />
      </ToastProvider>
    );

    act(() => {
      fireEvent.click(screen.getByText("Show Success"));
    });

    expect(screen.getByText("Success message")).toBeInTheDocument();

    act(() => {
      jest.advanceTimersByTime(2000);
    });

    await waitFor(() => {
      expect(screen.queryByText("Success message")).not.toBeInTheDocument();
    });
  });

  it("should limit the number of toasts to 5 and avoid key duplication", () => {
    render(
      <ToastProvider>
        <TestConsumer />
      </ToastProvider>
    );

    act(() => {
      const btn = screen.getByText("Show Default");
      for (let i = 0; i < 10; i++) {
        // Przesuwamy czas o 1ms, aby Date.now() wygenerowało unikalne ID (klucze)
        jest.advanceTimersByTime(1);
        fireEvent.click(btn);
      }
    });

    const toasts = screen.getAllByText("Default message");
    expect(toasts.length).toBe(5);
  });

  it("should remove toast when close button is clicked", async () => {
    render(
      <ToastProvider>
        <TestConsumer />
      </ToastProvider>
    );

    act(() => {
      fireEvent.click(screen.getByText("Show Default"));
    });

    expect(screen.getByText("Default message")).toBeInTheDocument();

    // Szukamy przycisku po selektorze klasy (standard w lucide-react buttons)
    const closeButton = screen.getByRole("button", { name: "" });

    act(() => {
      fireEvent.click(closeButton);
    });

    await waitFor(() => {
      expect(screen.queryByText("Default message")).not.toBeInTheDocument();
    });
  });

  it("should render correct styles for success and error", () => {
    render(
      <ToastProvider>
        <TestConsumer />
      </ToastProvider>
    );

    act(() => {
      fireEvent.click(screen.getByText("Show Success"));
      fireEvent.click(screen.getByText("Show Error"));
    });

    // Sprawdzamy obecność kontenerów z odpowiednimi klasami kolorów
    const successIcon = document.querySelector(".text-green-500");
    const errorIcon = document.querySelector(".text-red-500");

    expect(successIcon).toBeInTheDocument();
    expect(errorIcon).toBeInTheDocument();
  });

  it("should throw error when useToast is used outside provider", () => {
    const consoleSpy = jest
      .spyOn(console, "error")
      .mockImplementation(() => {});

    const ComponentOutside = () => {
      useToast();
      return null;
    };

    expect(() => render(<ComponentOutside />)).toThrow(
      "useToast must be used within a ToastProvider"
    );

    consoleSpy.mockRestore();
  });
});
