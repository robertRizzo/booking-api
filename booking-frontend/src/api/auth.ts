import apiClient from "./client";
import type { AuthRequest, AuthResponse, RegisterRequest, UserResponse } from "../types";

export async function loginUser(data: AuthRequest): Promise<AuthResponse>
{
    const response = await apiClient.post<AuthResponse>("/api/auth/login", data);
    return response.data;
}

export async function registerUser(data: RegisterRequest): Promise<UserResponse>
{
    const response = await apiClient.post<UserResponse>("/api/auth/register", data);
    return response.data;
}

