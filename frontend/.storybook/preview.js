// .storybook/preview.js

import "../src/index.css";
import "bootstrap/dist/css/bootstrap.css";
import 'react-toastify/dist/ReactToastify.css';

import { initialize, mswLoader } from 'msw-storybook-addon'

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, useLocation, Routes, Route } from "react-router";
import { ToastContainer, toast } from "react-toastify";
import { useEffect } from "react";

const queryClient = new QueryClient();

// Initialize MSW
initialize()

// The order of decorators here is crucial.
// The innermost decorator is applied first.
export const decorators = [
  // 4. This is the top-level decorator that provides react-query context.
  (Story) => (
    <QueryClientProvider client={queryClient}>
      <Story />
    </QueryClientProvider >
  ),
  // 3. This decorator provides the toast container for pop-up messages.
  (Story) => {
    return (<>
      <ToastContainer />
      <Story />
    </>
    );
  },
  // 2. This is our custom router decorator. It conditionally applies the router.
  //    It now also uses Routes and Route to handle path parameters.
  (Story, Context) => {
    if (Context.args?.suppressMemoryRouter) {
      return <Story />;
    }
    
    // Get the routing parameters from the story parameters
    const { routing } = Context.parameters;

    // Use the path from the parameters, or a default if not provided
    const path = routing?.path || "/";
    const initialEntries = [routing?.initialEntries || "/"];

    // Render the Story inside the router context
    // We use a Routes/Route setup to match the path and make path parameters available
    return (
      <MemoryRouter initialEntries={initialEntries}>
        <Routes>
          <Route path={path} element={<Story />} />
        </Routes>
      </MemoryRouter>
    );
  },
  // 1. This is the innermost decorator for showing toast notifications on navigation.
  (Story, Context) => {
    if (Context.args?.suppressMemoryRouter) {
      return <Story />;
    } else {
      const location = useLocation();
      useEffect(() => {
        if (location.pathname !== "/") {
          toast("Would navigate to: " + location.pathname);
        }
      }, [location]);
      return <Story />;
    }
  },
];


/** @type { import('@storybook/react-webpack5').Preview } */
const preview = {
  parameters: {
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
  },
  loaders: [mswLoader]
};


export default preview;