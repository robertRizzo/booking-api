import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { TextInput, PasswordInput, Button, Paper, Title, Text, Stack } from "@mantine/core";
import { useAuth } from "../context/AuthContext";
import { isAxiosError } from "axios";

export default function RegisterPage()
{
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    const { register } = useAuth();
    const navigate = useNavigate();

    async function handleSubmit(e: React.SyntheticEvent<HTMLFormElement>)
    {
        e.preventDefault();
        setError("");
        if (password != confirmPassword)
        {
            setError("Passwords do not match");
            return;
        }
        setLoading(true);

        try
        {
            await register({ email, password });
            navigate("/dashboard");
        }
        catch (err: unknown)
        {
            if (isAxiosError(err))
            {
                setError(err.response?.data?.message || "Login failed");
            }
            else
            {
                setError("Login failed");
            }
        }
        finally
        {
            setLoading(false);
        }
    }

    return (
        <Paper shadow="md" p="lg" radius="md" withBorder>
            <Title order={2}>Register</Title>

            <form onSubmit={handleSubmit}>
                <Stack>

                    <TextInput
                        label="Email"
                        value={email}
                        onChange={(e) => setEmail(e.currentTarget.value)}
                        required
                    />

                    <PasswordInput
                        label="Password"
                        value={password}
                        onChange={(e) => setPassword(e.currentTarget.value)}
                        required
                    />
                    <PasswordInput 
                        label="Confirm Password"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.currentTarget.value)}
                        required
                    />

                    {error && <Text c="red">{error}</Text>}

                    <Button type="submit" loading={loading}>
                        Register
                    </Button>

                    <Text size="sm">
                        Already have an account? <Link to="/login">Login</Link>
                    </Text>

                </Stack>
            </form>
        </Paper>
    );
}