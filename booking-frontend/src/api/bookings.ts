import apiClient from "./client";
import type { BookingRequest, BookingResponse } from "../types";

export async function getAllBookings(): Promise<BookingResponse[]>
{
    const res = await apiClient.get<BookingResponse[]>("/api/bookings");
    return res.data;
}

export async function getMyBookings(): Promise<BookingResponse[]>
{
    const res = await apiClient.get<BookingResponse[]>("/api/bookings/my")
    return res.data;
}

export async function getBookingById(id: number): Promise<BookingResponse>
{
    const res = await apiClient.get<BookingResponse>(`/api/bookings/${id}`);
    return res.data;
}

export async function createBooking(data: BookingRequest): Promise<BookingResponse>
{
    const res = await apiClient.post<BookingResponse>("/api/bookings", data);
    return res.data;
}

export async function updateBooking(id: number, data: BookingRequest): Promise<BookingResponse>
{
    const res = await apiClient.put<BookingResponse>(`/api/bookings/${id}`, data);
    return res.data;
}

export async function cancelBooking(id: number): Promise<void>
{
    await apiClient.delete(`/api/bookings/${id}`);
}