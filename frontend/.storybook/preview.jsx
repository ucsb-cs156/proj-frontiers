import "bootstrap/dist/css/bootstrap.css";
import 'react-toastify/dist/ReactToastify.css';
import "../src/index.css";

import { mswLoader } from "msw-storybook-addon/csf3";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, useLocation } from "react-router";
import { ToastContainer, toast } from "react-toastify";
import { useEffect, useState } from "react";


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
  (Story) => {
    // Create a fresh QueryClient per story rather than one shared for the
    // whole Storybook session (issue #697). Storybook is a single-page app
    // and does not reload when navigating between stories via the sidebar,
    // so a module-scope QueryClient keeps its cache across stories. Stories
    // that share a query key (e.g. the InstructorCourseShowPage stories,
    // which all fetch /api/rosterStudents/course/7 with different mocked
    // data) would then briefly show a previous story's cached data instead
    // of their own until a full page refresh cleared the cache.
    const [queryClient] = useState(() => new QueryClient());
    return (
      <QueryClientProvider client={queryClient}>
        <Story />
      </QueryClientProvider>
    );
  },
];


/** @type { import('@storybook/react-vite').Preview } */
const preview = {
  parameters: {
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
  },
  loaders: [mswLoader()],
};


export default preview;
