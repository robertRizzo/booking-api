import axios from "axios";
import { AxiosError } from "axios";
import { notifications } from "@mantine/notifications";

const apiClient = axios.create
({
    baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080",
    headers: 
    {
        "Content-Type": "application/json",
    },
});

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const status = error.response?.status;

    if (status === 401) {
      localStorage.removeItem("token");
      window.location.href = "/login";
    } else if (status === 403) {
      notifications.show({
        title: "Access Denied",
        message: "You don't have permission to perform this action",
        color: "red",
      });
    }

    return Promise.reject(error);
  }
);

apiClient.interceptors.request.use((config) =>
{
    const token = localStorage.getItem("token");
    if (token)
    {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

export default apiClient;