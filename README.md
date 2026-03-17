# BookingAPI

## Project Overview

BokingAPI is a backend reservation system designed to demonstrate modern backend development practices inclusing authentication, databas managememt, testing, and containerization.

The system allows users to create and manage bookings while preventing booking conflicts.

---

## Architecture

The system follows a layered architechture:

Client -> REST API -> Service Layer -> Repository Layer -> PostgreSQL Database

The database is containerized using Docker for consistent development enviorments.

---

## Tech Stack

Backend:
- Java 17
- Spring Boot 3
- Spring Security 
- Spring Data JPA

Database:
- PostgreSQL

DevOps:
- Docker
- Docker Compose

Testing:
- JUnit 5
- Mockito

---

## Running the Project

###  Step 1 - Start Database Container
Make sure Docker is running, then execute:
    docker compose up -d

Verify container is running:
    docker ps

You should see the PostgreSQL container.

---

### Step 2 - Run Backend Application

Option A - Using IDE (Recomennded)
- Open:
BookingApiApplication.java
- Run the main class directly from your IDE.

Option B - Using Maven Terminal
Run in project root:
    mvn spring-boot:run

---

### Step 3 - Verify Application

Open browser:
    http://localhost:8080/actuator/health

If working correctly you should see:
    {"status":"UP"}

--- 

"" Project Goals

- Secure authentication using JWT
- Booking conflict detection
- Production-like backend architechture
- Demonstrate software engineering best practices

## Troubleshooting

If application failes to start:

- Verify Docker container is running
- Check database credentials in application.yml
- Ensure port 5432 is free

---

## Fure Improvments

- Add CI/CD pipeline
- Add catching layer
- Add messaging/event-driven architechture
- Add advanced monitoring

---

## Author

Robert Rizzo
