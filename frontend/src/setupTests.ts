import "@testing-library/jest-dom";
import { TextEncoder, TextDecoder } from "util";
import React from "react";

// 1. Rozwiązanie błędu ReferenceError: TextEncoder is not defined
global.TextEncoder = TextEncoder;
// @ts-ignore
global.TextDecoder = TextDecoder;

// 2. Mockowanie ResizeObserver
global.ResizeObserver = class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
};

// 3. Mockowanie obiektu import.meta (Vite)
// @ts-ignore
global.import = {
  meta: {
    env: {
      VITE_API_URL: "/api/auth",
      VITE_MENU_API_URL: "/api/menu",
      VITE_MAKAO_API_URL: "/api/makao",
      VITE_API_SOCIAL_WS_URL: "/api/social/ws/presence",
      VITE_MENU_WS_URL: "/api/menu/ws",
    },
  },
};

// 4. Bezpieczny mock dla framer-motion (Naprawia ostrzeżenia act())
// Używamy prostych funkcji, aby uniknąć problemów z parserem Regex
jest.mock("framer-motion", () => {
  return {
    AnimatePresence: (props: any) =>
      React.createElement(React.Fragment, null, props.children),
    motion: {
      div: React.forwardRef((props: any, ref: any) => {
        const { children, ...rest } = props;
        // Usuwamy właściwości specyficzne dla framer-motion, które mogą psuć czysty tag div
        const cleanProps = Object.keys(rest)
          .filter(
            (key) =>
              !["initial", "animate", "exit", "transition", "layout"].includes(
                key
              )
          )
          .reduce((obj: any, key) => {
            obj[key] = rest[key];
            return obj;
          }, {});

        return React.createElement("div", { ...cleanProps, ref }, children);
      }),
    },
  };
});
