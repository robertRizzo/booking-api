import apiClient from "./client";
import type { RoomRequest, RoomResponse } from "../types";

export async function getRooms(): Promise<RoomResponse[]>
{
    const res = await apiClient.get<RoomResponse[]>("/api/rooms");
    return res.data;
}

export async function getRoomById(id: number): Promise<RoomResponse>
{
    const res = await apiClient.get<RoomResponse>(`/api/rooms/${id}`);
    return res.data;
}

export async function createRoom(data: RoomRequest): Promise<RoomResponse>
{
    const res = await apiClient.post<RoomResponse>("/api/rooms", data);
    return res.data;
}

export async function updateRoom(id: number, data: RoomRequest): Promise<RoomResponse>
{
    const res = await apiClient.put<RoomResponse>(`/api/rooms/${id}`, data);
    return res.data;
}

export async function deleteRoom(id: number): Promise<void>
{
    await apiClient.delete(`/api/rooms/${id}`);
}