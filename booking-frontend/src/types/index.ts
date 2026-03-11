// -- Auth --
export interface AuthRequest
{
    email: string;
    password: string;
}

export interface AuthResponse
{
    token: string;
}

export interface RegisterRequest
{
    email: string;
    password: string;
}

// -- Users --
export interface UserRequest
{
    email: string;
    password: string;
    role?: string;
}

export interface UserResponse
{
    id: number;
    email: String;
    role: string;
}

// -- Rooms --
export interface RoomRequest
{
    name: string;
    capacity: number;
}

export interface RoomResponse
{
    id: number;
    name: string;
    capacity: number;
}

// -- Bookings --
export interface BookingRequest
{
    userId: number;
    roomId: number;
    startTime: string; // ISO-8601
    endTime: string; // ISO-8601
}

export interface BookingResponse
{
    id: number;
    userId: number;
    userEmail: string;
    roomId: number;
    roomName: string;
    startTime: string;
    endTime: string; 
    status: string;
}

// -- Errors --
export interface ErrorResponse
{
    status: number;
    message: string;
    timeStamp: string;
}