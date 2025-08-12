import "bootstrap/dist/css/bootstrap.css";
import 'react-toastify/dist/ReactToastify.css';
import "../src/index.css";

import { initialize, mswLoader } from 'msw-storybook-addon'

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, useLocation } from "react-router";
import { ToastContainer, toast } from "react-toastify";
import { useEffect } from "react";

const queryClient = new QueryClient();

// Initialize MSW
initialize()

// For conditional decorators trick, see: https://github.com/storybookjs/storybook/issues/23237#issuecomment-1611351405 
// Decorators are applied in order; the innermost decorator is applied first.
// Here, if suppressMemoryRouter is true, then the MemoryRouter decorator is not applied,
// and we don't use the useLocation hook to show a toast message when navigate is called.

export const decorators = [
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
  (Story) => {
    return (<>
      <ToastContainer />
      <Story />
    </>
    );
  },
  (Story, Context) => (
    Context.args?.suppressMemoryRouter ?
      <Story /> :
      <MemoryRouter><Story /></MemoryRouter>
  ),
  (Story) => (
    <QueryClientProvider client={queryClient}>
      <Story />
    </QueryClientProvider >
  ),
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
