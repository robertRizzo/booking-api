interface JwtPayload
{
    sub: string; // email
    role: string;
    iat: number;
    exp: number;
}

export function decodeToken(token: string): JwtPayload
{
    const payload = token.split(".")[1];
    return JSON.parse(atob(payload));
}