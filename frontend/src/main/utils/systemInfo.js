import { useQuery } from '@tanstack/react-query';
import axios from "axios";

export function useSystemInfo() {
  return useQuery({
    queryKey: ["systemInfo"],
    queryFn: async () => {
      try {
        const response = await axios.get("/api/systemInfo");
        return response.data;
      } catch (e) {
        console.error("Error invoking axios.get: ", e);
        return {};
      }
    },
    initialData: {
      initialData: true,
      springH2ConsoleEnabled: false,
      showSwaggerUILink: false,
    },
  });
}
