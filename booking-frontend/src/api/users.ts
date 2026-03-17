import apiClient from "./client";
import type { UserRequest, UserResponse } from "../types";

export async function getUsers(): Promise<UserResponse[]>
{
    const res = await apiClient.get<UserResponse[]>("/api/users");
    return res.data;
}

export async function getUserById(id: number): Promise<UserResponse>
{
  const res = await apiClient.get<UserResponse>(`/api/users/${id}`);
  return res.data;
}

export async function createUser(data: UserRequest): Promise<UserResponse>
{
  const res = await apiClient.post<UserResponse>("/api/users", data);
  return res.data;
}

export async function updateUser(id: number, data: UserRequest): Promise<UserResponse>
{
  const res = await apiClient.put<UserResponse>(`/api/users/${id}`, data);
  return res.data;
}

export async function deleteUser(id: number): Promise<void>
{
  await apiClient.delete(`/api/users/${id}`);
}