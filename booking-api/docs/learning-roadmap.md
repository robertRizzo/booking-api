# Booking API - Learning Roadmap

## Background

BookingAPI is a secure backend system for managing reservations, users, and resources with conflict detection and role-based access control. The system is built with Spring Boot, PostgreSQL, Docker, Flyway, and Spring Security.

## Overall Goal

Build a production-grade REST API that supports:

- User authentication with JWT tokens
- Role-based authorization (ADMIN, USER)
- Full booking lifecycle management (create, read, update, cancel)
- Resource management (rooms)
- Booking conflict detection
- Automated testing
- Containerized deployment

## Definition of Done

The system is complete when:

1. All REST endpoints return correct responses for valid and invalid input
2. JWT authentication protects all non-public endpoints
3. Role-based access control restricts operations by user role
4. Booking conflicts are detected and rejected
5. Unit and integration tests cover core business logic
6. The full stack (app + database) runs via `docker compose up`
7. The actuator health endpoint returns `{"status":"UP"}`

---

## Phase 8 - Stabilize the Current System

### What You Will Learn

- How Spring Security's `PasswordEncoder` interacts with auto-configured users
- How Spring Boot YAML property hierarchy works
- How to verify a running system end-to-end

### What Is Wrong

There are two configuration issues preventing the application from starting and authenticating correctly.

**Issue 1: `BCryptPasswordEncoder` breaks in-memory login**

In `SecurityConfig.java`, the `PasswordEncoder` bean returns a `BCryptPasswordEncoder`. When this bean exists, Spring Boot stores the configured password `"admin"` as-is (no `{noop}` prefix). At login, `BCryptPasswordEncoder.matches("admin", "admin")` fails because `"admin"` is not a valid BCrypt hash. Authentication is impossible.

**Issue 2: `management` section is nested under `spring:` in YAML**

In `application.yml`, the `management:` block is indented under `spring:`, producing the invalid property path `spring.management.endpoints.web.exposure.include`. Spring Boot ignores it. The health endpoint still works by default, but the configuration is silently wrong.

### Files to Modify

- `src/main/java/com/booking_api/config/SecurityConfig.java`
- `src/main/resources/application.yml`

### Step 1 - Fix SecurityConfig.java

Remove the `passwordEncoder()` bean and its imports. The file should contain only the `SecurityFilterChain` bean:

```java
package com.booking_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig
{
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception
    {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
```

You will re-add a `PasswordEncoder` bean later in Phase 12 when you implement JWT authentication with a database-backed `UserDetailsService`.

### Step 2 - Fix application.yml

Move the `management:` block to the top level (zero indentation), outside the `spring:` block:

```yaml
spring:
  security:
    user:
      name: admin
      password: admin
  datasource:
    url: jdbc:postgresql://localhost:5432/bookingdb
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: none
    show-sql: true

  flyway:
    enabled: true
    baseline-on-migrate: true

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

### Step 3 - Verify

```bash
docker compose up -d
./mvnw spring-boot:run
```

```bash
curl -u admin:admin http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

### Git Checkpoint

```
git add -A
git commit -m "fix: resolve authentication and actuator configuration issues"
```

---

## Phase 9 - Complete CRUD Operations

### What You Will Learn

- REST API design conventions (proper use of HTTP methods and status codes)
- Path variables and request bodies in Spring MVC
- Service layer patterns for CRUD operations
- How Spring Data JPA repository methods translate to SQL

### What to Build

Full CRUD endpoints for three resources: Users, Rooms, and Bookings.

### REST Endpoint Reference

Design your endpoints following this structure:

| Resource | Method | Path | Description |
|----------|--------|------|-------------|
| Users | GET | /api/users | List all users |
| Users | GET | /api/users/{id} | Get user by ID |
| Users | POST | /api/users | Create user |
| Users | PUT | /api/users/{id} | Update user |
| Users | DELETE | /api/users/{id} | Delete user |
| Rooms | GET | /api/rooms | List all rooms |
| Rooms | GET | /api/rooms/{id} | Get room by ID |
| Rooms | POST | /api/rooms | Create room |
| Rooms | PUT | /api/rooms/{id} | Update room |
| Rooms | DELETE | /api/rooms/{id} | Delete room |
| Bookings | GET | /api/bookings | List all bookings |
| Bookings | GET | /api/bookings/{id} | Get booking by ID |
| Bookings | POST | /api/bookings | Create booking |
| Bookings | PUT | /api/bookings/{id} | Update booking |
| Bookings | DELETE | /api/bookings/{id} | Cancel booking |

### Step 1 - Implement RoomService

Add CRUD methods to `RoomService.java`. Use the repository methods provided by `JpaRepository` (`findAll`, `findById`, `save`, `deleteById`).

Complete `src/main/java/com/booking_api/service/RoomService.java`:

```java
package com.booking_api.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.booking_api.model.Room;
import com.booking_api.repository.RoomRepository;

@Service
public class RoomService
{
    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository)
    {
        this.roomRepository = roomRepository;
    }

    public List<Room> getAllRooms()
    {
        return roomRepository.findAll();
    }

    public Room getRoomById(Long id)
    {
        return roomRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + id));
    }

    public Room createRoom(Room room)
    {
        return roomRepository.save(room);
    }

    public Room updateRoom(Long id, Room roomDetails)
    {
        Room existing = roomRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + id));

        existing.setName(roomDetails.getName());
        existing.setCapacity(roomDetails.getCapacity());

        return roomRepository.save(existing);
    }

    public void deleteRoom(Long id)
    {
        if (!roomRepository.existsById(id))
        {
            throw new IllegalArgumentException("Room not found with id: " + id);
        }
        roomRepository.deleteById(id);
    }
}
```

Key patterns to understand:
- `findById` returns `Optional<Room>`. Use `orElseThrow` to convert it to the entity or throw if missing.
- `updateRoom` fetches the existing entity first, mutates its fields, then saves. This ensures JPA tracks the change as an UPDATE, not an INSERT.
- `deleteRoom` checks existence before deleting to provide a clear error message. `deleteById` silently does nothing if the ID does not exist.

### Step 2 - Implement RoomController

Wire each service method to a REST endpoint. Use `@PathVariable` for ID parameters and `@RequestBody` for request payloads. Return `ResponseEntity` to control HTTP status codes.

Complete `src/main/java/com/booking_api/controller/RoomController.java`:

```java
package com.booking_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking_api.model.Room;
import com.booking_api.service.RoomService;

@RestController
@RequestMapping("/api/rooms")
public class RoomController
{
    private final RoomService roomService;

    public RoomController(RoomService roomService)
    {
        this.roomService = roomService;
    }

    @GetMapping
    public ResponseEntity<List<Room>> getAllRooms()
    {
        List<Room> rooms = roomService.getAllRooms();
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Room> getRoomById(@PathVariable Long id)
    {
        Room room = roomService.getRoomById(id);
        return ResponseEntity.ok(room);
    }

    @PostMapping
    public ResponseEntity<Room> createRoom(@RequestBody Room room)
    {
        Room created = roomService.createRoom(room);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Room> updateRoom(@PathVariable Long id, @RequestBody Room room)
    {
        Room updated = roomService.updateRoom(id, room);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id)
    {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }
}
```

Key patterns to understand:
- POST returns `201 Created` with the new resource in the body.
- PUT returns `200 OK` with the updated resource.
- DELETE returns `204 No Content` with an empty body. The `Void` type parameter tells Spring there is no response body.
- `@PathVariable` binds the `{id}` segment from the URL path to the method parameter.

### Step 3 - Implement UserService and UserController

Complete `src/main/java/com/booking_api/service/UserService.java`:

```java
package com.booking_api.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.booking_api.model.User;
import com.booking_api.repository.UserRepository;

@Service
public class UserService
{
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository)
    {
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers()
    {
        return userRepository.findAll();
    }

    public User getUserById(Long id)
    {
        return userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
    }

    public User createUser(User user)
    {
        return userRepository.save(user);
    }

    public User updateUser(Long id, User userDetails)
    {
        User existing = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        existing.setEmail(userDetails.getEmail());
        existing.setPassword(userDetails.getPassword());
        existing.setRole(userDetails.getRole());

        return userRepository.save(existing);
    }

    public void deleteUser(Long id)
    {
        if (!userRepository.existsById(id))
        {
            throw new IllegalArgumentException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}
```

Complete `src/main/java/com/booking_api/controller/UserController.java`:

```java
package com.booking_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking_api.model.User;
import com.booking_api.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController
{
    private final UserService userService;

    public UserController(UserService userService)
    {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers()
    {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id)
    {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user)
    {
        User created = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user)
    {
        User updated = userService.updateUser(id, user);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id)
    {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
```

Notice how every controller follows the same structure: constructor injection, five endpoints, consistent return types. This repetition is intentional in CRUD APIs and becomes the pattern you will recognize across Spring Boot projects.

### Step 4 - Complete BookingService and BookingController

`BookingService` already has `createBooking`. Add the remaining CRUD methods.

Complete `src/main/java/com/booking_api/service/BookingService.java`:

```java
package com.booking_api.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.booking_api.model.Booking;
import com.booking_api.model.Status;
import com.booking_api.repository.BookingRepository;
import com.booking_api.repository.RoomRepository;
import com.booking_api.repository.UserRepository;

@Service
public class BookingService
{
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository,
                          RoomRepository roomRepository,
                          UserRepository userRepository)
    {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    public List<Booking> getAllBookings()
    {
        return bookingRepository.findAll();
    }

    public Booking getBookingById(Long id)
    {
        return bookingRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));
    }

    public Booking createBooking(Booking booking)
    {
        if (booking.getStartTime().isAfter(booking.getEndTime())
            || booking.getStartTime().isEqual(booking.getEndTime()))
        {
            throw new IllegalArgumentException("Invalid time range");
        }

        if (booking.getStartTime().isBefore(LocalDateTime.now()))
        {
            throw new IllegalArgumentException("Booking cannot start in the past");
        }

        userRepository.findById(booking.getUser().getId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        roomRepository.findById(booking.getRoom().getId())
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        List<Booking> conflicts = bookingRepository.findConflictingBookings(
            booking.getRoom().getId(),
            booking.getStartTime(),
            booking.getEndTime()
        );

        if (!conflicts.isEmpty())
        {
            throw new IllegalStateException("Room is already booked during this time");
        }

        booking.setStatus(Status.CONFIRMED);
        return bookingRepository.save(booking);
    }

    public Booking updateBooking(Long id, Booking bookingDetails)
    {
        Booking existing = bookingRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));

        if (bookingDetails.getStartTime().isAfter(bookingDetails.getEndTime())
            || bookingDetails.getStartTime().isEqual(bookingDetails.getEndTime()))
        {
            throw new IllegalArgumentException("Invalid time range");
        }

        if (bookingDetails.getStartTime().isBefore(LocalDateTime.now()))
        {
            throw new IllegalArgumentException("Booking cannot start in the past");
        }

        List<Booking> conflicts = bookingRepository.findConflictingBookings(
            bookingDetails.getRoom().getId(),
            bookingDetails.getStartTime(),
            bookingDetails.getEndTime()
        );

        conflicts.removeIf(b -> b.getId().equals(id));

        if (!conflicts.isEmpty())
        {
            throw new IllegalStateException("Room is already booked during this time");
        }

        existing.setRoom(bookingDetails.getRoom());
        existing.setStartTime(bookingDetails.getStartTime());
        existing.setEndTime(bookingDetails.getEndTime());

        return bookingRepository.save(existing);
    }

    public void cancelBooking(Long id)
    {
        Booking existing = bookingRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));

        existing.setStatus(Status.CANCELLED);
        bookingRepository.save(existing);
    }
}
```

Key design decisions:
- `createBooking` sets the status to `CONFIRMED` automatically. The client does not control the initial status.
- `updateBooking` revalidates time range and conflict rules. It filters out the current booking from the conflict list so a booking does not conflict with itself.
- `cancelBooking` does not delete the row. It sets the status to `CANCELLED`, preserving an audit trail.

Complete `src/main/java/com/booking_api/controller/BookingController.java`:

```java
package com.booking_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking_api.model.Booking;
import com.booking_api.service.BookingService;

@RestController
@RequestMapping("/api/bookings")
public class BookingController
{
    private final BookingService bookingService;

    public BookingController(BookingService bookingService)
    {
        this.bookingService = bookingService;
    }

    @GetMapping
    public ResponseEntity<List<Booking>> getAllBookings()
    {
        List<Booking> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Booking> getBookingById(@PathVariable Long id)
    {
        Booking booking = bookingService.getBookingById(id);
        return ResponseEntity.ok(booking);
    }

    @PostMapping
    public ResponseEntity<Booking> createBooking(@RequestBody Booking booking)
    {
        Booking created = bookingService.createBooking(booking);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Booking> updateBooking(@PathVariable Long id, @RequestBody Booking booking)
    {
        Booking updated = bookingService.updateBooking(id, booking);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long id)
    {
        bookingService.cancelBooking(id);
        return ResponseEntity.noContent().build();
    }
}
```

The DELETE endpoint calls `cancelBooking` (soft delete) rather than a hard delete. The HTTP contract remains the same: the client sends `DELETE /api/bookings/1` and receives `204 No Content`.

### Step 5 - Test Manually

```bash
# Create a room
curl -u admin:admin -X POST http://localhost:8080/api/rooms \
  -H "Content-Type: application/json" \
  -d '{"name": "Conference Room A", "capacity": 10}'

# List rooms
curl -u admin:admin http://localhost:8080/api/rooms
```

### Git Checkpoint

```
git add -A
git commit -m "feat: implement CRUD operations for users, rooms, and bookings"
```

---

## Phase 10 - DTOs and Input Validation

### What You Will Learn

- Why you never expose JPA entities directly in API responses
- How to use Data Transfer Objects to control input/output
- Bean Validation annotations (`@NotNull`, `@NotBlank`, `@Size`, `@Future`)
- How `@Valid` triggers validation in controllers

### Why DTOs Matter

Exposing JPA entities directly creates several problems:
- Clients can set fields they should not control (like `id`, `role`, `status`)
- Circular references between entities (User -> Bookings -> User) cause infinite JSON serialization
- Internal fields (like password hashes) leak to the client
- You cannot evolve your database schema independently from your API contract

### Step 1 - Create a DTO Package

Create the package `com.booking_api.dto` with request and response DTOs.

Start with rooms since they are the simplest:

```java
// src/main/java/com/booking_api/dto/RoomRequest.java
package com.booking_api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoomRequest(
    @NotBlank(message = "Room name is required")
    String name,

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    Integer capacity
) {}
```

```java
// src/main/java/com/booking_api/dto/RoomResponse.java
package com.booking_api.dto;

public record RoomResponse(
    Long id,
    String name,
    Integer capacity
) {}
```

Java records are ideal for DTOs: they are immutable, concise, and automatically generate `equals`, `hashCode`, and `toString`.

### Step 2 - Create Booking DTOs

```java
// src/main/java/com/booking_api/dto/BookingRequest.java
package com.booking_api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record BookingRequest(
    @NotNull(message = "User ID is required")
    Long userId,

    @NotNull(message = "Room ID is required")
    Long roomId,

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    LocalDateTime startTime,

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    LocalDateTime endTime
) {}
```

```java
// src/main/java/com/booking_api/dto/BookingResponse.java
package com.booking_api.dto;

import java.time.LocalDateTime;

public record BookingResponse(
    Long id,
    Long userId,
    String userEmail,
    Long roomId,
    String roomName,
    LocalDateTime startTime,
    LocalDateTime endTime,
    String status
) {}
```

### Step 3 - Create User DTOs

```java
// src/main/java/com/booking_api/dto/UserRequest.java
package com.booking_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    String password,

    String role
) {}
```

```java
// src/main/java/com/booking_api/dto/UserResponse.java
package com.booking_api.dto;

public record UserResponse(
    Long id,
    String email,
    String role
) {}
```

The `UserResponse` deliberately excludes `password`. This is the primary reason DTOs exist for the User entity: you never want to serialize a password hash to the client.

The `role` field in `UserRequest` is a plain `String` (not the `Role` enum) so the service layer can parse and validate it. Making the request DTO accept a `String` keeps the API contract independent from your internal enum names.

### Step 4 - Update Services to Use DTOs

Every service method now accepts a request DTO and returns a response DTO. The service handles conversion between DTO and entity in both directions.

Complete `src/main/java/com/booking_api/service/RoomService.java` (DTO version):

```java
package com.booking_api.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.booking_api.dto.RoomRequest;
import com.booking_api.dto.RoomResponse;
import com.booking_api.model.Room;
import com.booking_api.repository.RoomRepository;

@Service
public class RoomService
{
    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository)
    {
        this.roomRepository = roomRepository;
    }

    public List<RoomResponse> getAllRooms()
    {
        return roomRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public RoomResponse getRoomById(Long id)
    {
        Room room = roomRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + id));
        return toResponse(room);
    }

    public RoomResponse createRoom(RoomRequest request)
    {
        Room room = new Room();
        room.setName(request.name());
        room.setCapacity(request.capacity());

        Room saved = roomRepository.save(room);
        return toResponse(saved);
    }

    public RoomResponse updateRoom(Long id, RoomRequest request)
    {
        Room existing = roomRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + id));

        existing.setName(request.name());
        existing.setCapacity(request.capacity());

        Room saved = roomRepository.save(existing);
        return toResponse(saved);
    }

    public void deleteRoom(Long id)
    {
        if (!roomRepository.existsById(id))
        {
            throw new IllegalArgumentException("Room not found with id: " + id);
        }
        roomRepository.deleteById(id);
    }

    private RoomResponse toResponse(Room room)
    {
        return new RoomResponse(room.getId(), room.getName(), room.getCapacity());
    }
}
```

The `toResponse` private helper keeps the conversion in one place. If you add fields to the response DTO later, you only change this one method.

Complete `src/main/java/com/booking_api/service/UserService.java` (DTO version):

```java
package com.booking_api.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.booking_api.dto.UserRequest;
import com.booking_api.dto.UserResponse;
import com.booking_api.model.Role;
import com.booking_api.model.User;
import com.booking_api.repository.UserRepository;

@Service
public class UserService
{
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository)
    {
        this.userRepository = userRepository;
    }

    public List<UserResponse> getAllUsers()
    {
        return userRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public UserResponse getUserById(Long id)
    {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        return toResponse(user);
    }

    public UserResponse createUser(UserRequest request)
    {
        User user = new User();
        user.setEmail(request.email());
        user.setPassword(request.password());
        user.setRole(parseRole(request.role()));

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    public UserResponse updateUser(Long id, UserRequest request)
    {
        User existing = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        existing.setEmail(request.email());
        existing.setPassword(request.password());
        existing.setRole(parseRole(request.role()));

        User saved = userRepository.save(existing);
        return toResponse(saved);
    }

    public void deleteUser(Long id)
    {
        if (!userRepository.existsById(id))
        {
            throw new IllegalArgumentException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    private Role parseRole(String role)
    {
        if (role == null || role.isBlank())
        {
            return Role.USER;
        }
        try
        {
            return Role.valueOf(role.toUpperCase());
        }
        catch (IllegalArgumentException e)
        {
            throw new IllegalArgumentException("Invalid role: " + role + ". Must be USER or ADMIN");
        }
    }

    private UserResponse toResponse(User user)
    {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole().name());
    }
}
```

Note: In this Phase 9/10 version, the password is stored in plain text. In Phase 12, you will hash it with `BCryptPasswordEncoder` before saving.

Complete `src/main/java/com/booking_api/service/BookingService.java` (DTO version):

```java
package com.booking_api.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.booking_api.dto.BookingRequest;
import com.booking_api.dto.BookingResponse;
import com.booking_api.model.Booking;
import com.booking_api.model.Room;
import com.booking_api.model.Status;
import com.booking_api.model.User;
import com.booking_api.repository.BookingRepository;
import com.booking_api.repository.RoomRepository;
import com.booking_api.repository.UserRepository;

@Service
public class BookingService
{
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository,
                          RoomRepository roomRepository,
                          UserRepository userRepository)
    {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    public List<BookingResponse> getAllBookings()
    {
        return bookingRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public BookingResponse getBookingById(Long id)
    {
        Booking booking = bookingRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));
        return toResponse(booking);
    }

    public BookingResponse createBooking(BookingRequest request)
    {
        if (request.startTime().isAfter(request.endTime())
            || request.startTime().isEqual(request.endTime()))
        {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        if (request.startTime().isBefore(LocalDateTime.now()))
        {
            throw new IllegalArgumentException("Booking cannot start in the past");
        }

        User user = userRepository.findById(request.userId())
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + request.userId()));

        Room room = roomRepository.findById(request.roomId())
            .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + request.roomId()));

        List<Booking> conflicts = bookingRepository.findConflictingBookings(
            room.getId(), request.startTime(), request.endTime());

        if (!conflicts.isEmpty())
        {
            throw new IllegalStateException("Room is already booked during this time");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoom(room);
        booking.setStartTime(request.startTime());
        booking.setEndTime(request.endTime());
        booking.setStatus(Status.CONFIRMED);

        Booking saved = bookingRepository.save(booking);
        return toResponse(saved);
    }

    public BookingResponse updateBooking(Long id, BookingRequest request)
    {
        Booking existing = bookingRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));

        if (request.startTime().isAfter(request.endTime())
            || request.startTime().isEqual(request.endTime()))
        {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        if (request.startTime().isBefore(LocalDateTime.now()))
        {
            throw new IllegalArgumentException("Booking cannot start in the past");
        }

        Room room = roomRepository.findById(request.roomId())
            .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + request.roomId()));

        List<Booking> conflicts = bookingRepository.findConflictingBookings(
            room.getId(), request.startTime(), request.endTime());

        conflicts.removeIf(b -> b.getId().equals(id));

        if (!conflicts.isEmpty())
        {
            throw new IllegalStateException("Room is already booked during this time");
        }

        existing.setRoom(room);
        existing.setStartTime(request.startTime());
        existing.setEndTime(request.endTime());

        Booking saved = bookingRepository.save(existing);
        return toResponse(saved);
    }

    public void cancelBooking(Long id)
    {
        Booking existing = bookingRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));

        existing.setStatus(Status.CANCELLED);
        bookingRepository.save(existing);
    }

    private BookingResponse toResponse(Booking booking)
    {
        return new BookingResponse(
            booking.getId(),
            booking.getUser().getId(),
            booking.getUser().getEmail(),
            booking.getRoom().getId(),
            booking.getRoom().getName(),
            booking.getStartTime(),
            booking.getEndTime(),
            booking.getStatus().name()
        );
    }
}
```

The `toResponse` method flattens the entity relationships: instead of nesting a full `User` and `Room` object, the response contains only the IDs and display names. This prevents circular references and keeps the JSON response clean.

### Step 5 - Update Controllers to Use @Valid

Add `@Valid` before each `@RequestBody` parameter. This triggers Bean Validation. All controllers now use DTOs for input and output.

Complete `src/main/java/com/booking_api/controller/RoomController.java` (DTO version):

```java
package com.booking_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking_api.dto.RoomRequest;
import com.booking_api.dto.RoomResponse;
import com.booking_api.service.RoomService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/rooms")
public class RoomController
{
    private final RoomService roomService;

    public RoomController(RoomService roomService)
    {
        this.roomService = roomService;
    }

    @GetMapping
    public ResponseEntity<List<RoomResponse>> getAllRooms()
    {
        return ResponseEntity.ok(roomService.getAllRooms());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable Long id)
    {
        return ResponseEntity.ok(roomService.getRoomById(id));
    }

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody RoomRequest request)
    {
        RoomResponse response = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomResponse> updateRoom(@PathVariable Long id,
                                                   @Valid @RequestBody RoomRequest request)
    {
        return ResponseEntity.ok(roomService.updateRoom(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id)
    {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }
}
```

Complete `src/main/java/com/booking_api/controller/UserController.java` (DTO version):

```java
package com.booking_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking_api.dto.UserRequest;
import com.booking_api.dto.UserResponse;
import com.booking_api.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController
{
    private final UserService userService;

    public UserController(UserService userService)
    {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers()
    {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id)
    {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request)
    {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                                   @Valid @RequestBody UserRequest request)
    {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id)
    {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
```

Complete `src/main/java/com/booking_api/controller/BookingController.java` (DTO version):

```java
package com.booking_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking_api.dto.BookingRequest;
import com.booking_api.dto.BookingResponse;
import com.booking_api.service.BookingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bookings")
public class BookingController
{
    private final BookingService bookingService;

    public BookingController(BookingService bookingService)
    {
        this.bookingService = bookingService;
    }

    @GetMapping
    public ResponseEntity<List<BookingResponse>> getAllBookings()
    {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id)
    {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request)
    {
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookingResponse> updateBooking(@PathVariable Long id,
                                                         @Valid @RequestBody BookingRequest request)
    {
        return ResponseEntity.ok(bookingService.updateBooking(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long id)
    {
        bookingService.cancelBooking(id);
        return ResponseEntity.noContent().build();
    }
}
```

If validation fails (for example, a blank room name), Spring automatically returns a `400 Bad Request` response before your controller code runs. In Phase 11, you will customize the error response format using `@RestControllerAdvice`.

### Git Checkpoint

```
git add -A
git commit -m "refactor: add DTOs and input validation for all resources"
```

---

## Phase 11 - Global Error Handling

### What You Will Learn

- How `@RestControllerAdvice` centralizes exception handling
- How to return consistent error responses across all endpoints
- Standard HTTP status code usage (400, 404, 409, 500)

### Step 1 - Create an Error Response DTO

```java
// src/main/java/com/booking_api/dto/ErrorResponse.java
package com.booking_api.dto;

import java.time.LocalDateTime;

public record ErrorResponse(
    int status,
    String message,
    LocalDateTime timestamp
) {}
```

### Step 2 - Create a GlobalExceptionHandler

Create `src/main/java/com/booking_api/config/GlobalExceptionHandler.java`:

```java
package com.booking_api.config;

import com.booking_api.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler
{
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex)
    {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex)
    {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex)
    {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Validation failed");

        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            message,
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
```

### Step 3 - Create Custom Exceptions (optional but recommended)

Replace generic `IllegalArgumentException` usage in services with specific exceptions:

```java
// src/main/java/com/booking_api/exception/ResourceNotFoundException.java
package com.booking_api.exception;

public class ResourceNotFoundException extends RuntimeException
{
    public ResourceNotFoundException(String message)
    {
        super(message);
    }
}
```

```java
// src/main/java/com/booking_api/exception/BookingConflictException.java
package com.booking_api.exception;

public class BookingConflictException extends RuntimeException
{
    public BookingConflictException(String message)
    {
        super(message);
    }
}
```

Now update the `GlobalExceptionHandler` to handle these custom exceptions alongside the generic ones. Here is the complete file with all handlers:

Complete `src/main/java/com/booking_api/config/GlobalExceptionHandler.java`:

```java
package com.booking_api.config;

import com.booking_api.dto.ErrorResponse;
import com.booking_api.exception.BookingConflictException;
import com.booking_api.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler
{
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex)
    {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(BookingConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(BookingConflictException ex)
    {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex)
    {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex)
    {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Validation failed");

        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            message,
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex)
    {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred",
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

The handler order matters: Spring selects the most specific exception type first. `ResourceNotFoundException` matches before `Exception`. The generic `Exception` handler is a safety net so no unhandled error ever leaks a stack trace to the client.

### Step 4 - Update Services

Replace generic exceptions in all three services with the custom ones. Here is the updated `BookingService` as a reference (apply the same pattern to `RoomService` and `UserService`, replacing `IllegalArgumentException("...not found...")` with `ResourceNotFoundException`):

Complete `src/main/java/com/booking_api/service/BookingService.java` (with custom exceptions):

```java
package com.booking_api.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.booking_api.dto.BookingRequest;
import com.booking_api.dto.BookingResponse;
import com.booking_api.exception.BookingConflictException;
import com.booking_api.exception.ResourceNotFoundException;
import com.booking_api.model.Booking;
import com.booking_api.model.Room;
import com.booking_api.model.Status;
import com.booking_api.model.User;
import com.booking_api.repository.BookingRepository;
import com.booking_api.repository.RoomRepository;
import com.booking_api.repository.UserRepository;

@Service
public class BookingService
{
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository,
                          RoomRepository roomRepository,
                          UserRepository userRepository)
    {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    public List<BookingResponse> getAllBookings()
    {
        return bookingRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public BookingResponse getBookingById(Long id)
    {
        Booking booking = bookingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        return toResponse(booking);
    }

    public BookingResponse createBooking(BookingRequest request)
    {
        validateTimeRange(request.startTime(), request.endTime());

        User user = userRepository.findById(request.userId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.userId()));

        Room room = roomRepository.findById(request.roomId())
            .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + request.roomId()));

        List<Booking> conflicts = bookingRepository.findConflictingBookings(
            room.getId(), request.startTime(), request.endTime());

        if (!conflicts.isEmpty())
        {
            throw new BookingConflictException("Room is already booked during this time");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoom(room);
        booking.setStartTime(request.startTime());
        booking.setEndTime(request.endTime());
        booking.setStatus(Status.CONFIRMED);

        Booking saved = bookingRepository.save(booking);
        return toResponse(saved);
    }

    public BookingResponse updateBooking(Long id, BookingRequest request)
    {
        Booking existing = bookingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        validateTimeRange(request.startTime(), request.endTime());

        Room room = roomRepository.findById(request.roomId())
            .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + request.roomId()));

        List<Booking> conflicts = bookingRepository.findConflictingBookings(
            room.getId(), request.startTime(), request.endTime());

        conflicts.removeIf(b -> b.getId().equals(id));

        if (!conflicts.isEmpty())
        {
            throw new BookingConflictException("Room is already booked during this time");
        }

        existing.setRoom(room);
        existing.setStartTime(request.startTime());
        existing.setEndTime(request.endTime());

        Booking saved = bookingRepository.save(existing);
        return toResponse(saved);
    }

    public void cancelBooking(Long id)
    {
        Booking existing = bookingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        existing.setStatus(Status.CANCELLED);
        bookingRepository.save(existing);
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime)
    {
        if (startTime.isAfter(endTime) || startTime.isEqual(endTime))
        {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        if (startTime.isBefore(LocalDateTime.now()))
        {
            throw new IllegalArgumentException("Booking cannot start in the past");
        }
    }

    private BookingResponse toResponse(Booking booking)
    {
        return new BookingResponse(
            booking.getId(),
            booking.getUser().getId(),
            booking.getUser().getEmail(),
            booking.getRoom().getId(),
            booking.getRoom().getName(),
            booking.getStartTime(),
            booking.getEndTime(),
            booking.getStatus().name()
        );
    }
}
```

Two improvements in this version:
- Time range validation is extracted into `validateTimeRange` since both `createBooking` and `updateBooking` need it. This is the DRY principle: when you find duplicate logic, extract it.
- `IllegalArgumentException` is still used for bad input (invalid time range), while `ResourceNotFoundException` is used for missing entities and `BookingConflictException` for scheduling conflicts. This maps cleanly to HTTP status codes: 400, 404, and 409.

Apply the same replacement in `RoomService` and `UserService`: change every `new IllegalArgumentException("...not found...")` to `new ResourceNotFoundException("...not found...")`.

### Git Checkpoint

```
git add -A
git commit -m "feat: add global exception handling with consistent error responses"
```

---

## Phase 12 - JWT Authentication

### What You Will Learn

- How JWT (JSON Web Token) authentication works
- How to implement a database-backed `UserDetailsService`
- How to write a JWT utility class for token creation and validation
- How Spring Security filter chains process requests
- How to replace HTTP Basic with stateless token-based auth

### Architecture Overview

```
Client                    Server
  |                         |
  |-- POST /api/auth/login -->
  |   (email + password)    |
  |                         |-- validate credentials
  |                         |-- generate JWT
  |<-- 200 { token: "..." } |
  |                         |
  |-- GET /api/rooms ------>|
  |   Authorization:        |
  |   Bearer <token>        |-- JwtAuthFilter extracts token
  |                         |-- validate token
  |                         |-- set SecurityContext
  |                         |-- proceed to controller
  |<-- 200 [rooms...]       |
```

### Step 1 - Add the JWT Dependency

Add to `pom.xml` inside `<dependencies>`:

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

Run `./mvnw dependency:resolve` to download.

### Step 2 - Add JWT Configuration Properties

Add to `application.yml` under a new top-level `jwt:` block:

```yaml
jwt:
  secret: your-256-bit-secret-key-change-this-in-production-use-env-var
  expiration: 86400000
```

Create a config class to bind these properties:

```java
// src/main/java/com/booking_api/config/JwtProperties.java
package com.booking_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    String secret,
    long expiration
) {}
```

Enable property binding by adding `@EnableConfigurationProperties(JwtProperties.class)` to your main application class.

### Step 3 - Create JwtService

Complete `src/main/java/com/booking_api/service/JwtService.java`:

```java
package com.booking_api.service;

import java.util.Date;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.booking_api.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService
{
    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties)
    {
        this.jwtProperties = jwtProperties;
    }

    public String generateToken(UserDetails userDetails)
    {
        return Jwts.builder()
            .subject(userDetails.getUsername())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtProperties.expiration()))
            .signWith(getSigningKey())
            .compact();
    }

    public String extractUsername(String token)
    {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails)
    {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token)
    {
        Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration.before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver)
    {
        Claims claims = Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return claimsResolver.apply(claims);
    }

    private SecretKey getSigningKey()
    {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.secret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

How this class works:
- `generateToken` builds a JWT with the email as `subject`, current time as `issuedAt`, and an expiration calculated from the configured duration.
- `extractClaim` is a generic helper that parses the token once and applies a function to extract any claim. This avoids parsing the token multiple times.
- `getSigningKey` decodes the Base64-encoded secret from `application.yml` into an HMAC-SHA key. The same key is used for both signing and verification.

### Step 4 - Implement UserDetailsService

Modify `UserRepository` to add a finder method:

```java
Optional<User> findByEmail(String email);
```

Create `src/main/java/com/booking_api/service/CustomUserDetailsService.java`:

```java
package com.booking_api.service;

import com.booking_api.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService
{
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository)
    {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException
    {
        var user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPassword())
            .roles(user.getRole().name())
            .build();
    }
}
```

### Step 5 - Create JwtAuthenticationFilter

This filter intercepts every HTTP request, extracts the JWT from the `Authorization` header, validates it, and sets the Spring Security context so that downstream code (controllers, `@PreAuthorize`) knows who the caller is.

Complete `src/main/java/com/booking_api/config/JwtAuthenticationFilter.java`:

```java
package com.booking_api.config;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.booking_api.service.CustomUserDetailsService;
import com.booking_api.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter
{
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   CustomUserDetailsService userDetailsService)
    {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
        throws ServletException, IOException
    {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer "))
        {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String username = jwtService.extractUsername(token);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null)
        {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtService.isTokenValid(token, userDetails))
            {
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

How the filter fits into the request lifecycle:
1. Every request hits this filter before reaching any controller.
2. If there is no `Authorization: Bearer ...` header, the filter passes the request through unchanged. Unauthenticated requests will be rejected later by Spring Security's authorization rules.
3. If a token is present and valid, the filter creates a `UsernamePasswordAuthenticationToken` (which implements `Authentication`) and places it in the `SecurityContextHolder`. This is what `@PreAuthorize` and `Authentication` method parameters read from.
4. The `OncePerRequestFilter` base class guarantees this filter runs exactly once per request, even if the request is internally forwarded.

### Step 6 - Create AuthController and Auth DTOs

Complete `src/main/java/com/booking_api/dto/AuthRequest.java`:

```java
package com.booking_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @NotBlank(message = "Password is required")
    String password
) {}
```

Complete `src/main/java/com/booking_api/dto/AuthResponse.java`:

```java
package com.booking_api.dto;

public record AuthResponse(String token) {}
```

Complete `src/main/java/com/booking_api/controller/AuthController.java`:

```java
package com.booking_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking_api.dto.AuthRequest;
import com.booking_api.dto.AuthResponse;
import com.booking_api.service.CustomUserDetailsService;
import com.booking_api.service.JwtService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController
{
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          CustomUserDetailsService userDetailsService)
    {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request)
    {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String token = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(token));
    }
}
```

How the login flow works:
1. `authenticationManager.authenticate(...)` delegates to `DaoAuthenticationProvider`, which calls `CustomUserDetailsService.loadUserByUsername()` to load the user from the database, then uses `BCryptPasswordEncoder` to compare the submitted password against the stored hash.
2. If authentication fails (wrong email or password), Spring Security throws `BadCredentialsException`, which results in a `401 Unauthorized` response.
3. If authentication succeeds, we load the `UserDetails` again and pass it to `JwtService.generateToken()` to create a signed JWT.
4. The token is returned to the client, who includes it in subsequent requests as `Authorization: Bearer <token>`.

### Step 7 - Update SecurityConfig

Replace the HTTP Basic config with the JWT filter chain. Re-add the `PasswordEncoder` bean now that you have a database-backed `UserDetailsService`.

Complete `src/main/java/com/booking_api/config/SecurityConfig.java` (JWT version):

```java
package com.booking_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.booking_api.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
public class SecurityConfig
{
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          CustomUserDetailsService userDetailsService)
    {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception
    {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider()
    {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
        throws Exception
    {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder()
    {
        return new BCryptPasswordEncoder();
    }
}
```

What changed from the Phase 8 version:
- `httpBasic()` is removed. Authentication is now handled entirely by the JWT filter.
- `SessionCreationPolicy.STATELESS` tells Spring Security to never create an HTTP session. Every request must carry its own JWT. This is the standard approach for REST APIs.
- `addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)` inserts your custom filter into the filter chain before Spring's default form-login filter.
- `DaoAuthenticationProvider` connects `CustomUserDetailsService` (database lookup) with `BCryptPasswordEncoder` (password verification). The `AuthenticationManager` uses this provider when `AuthController` calls `authenticate()`.
- `/actuator/health` is now explicitly permitted without authentication, so health checks from load balancers or Docker work without credentials.

### Step 8 - Remove the Spring Security Default User

Delete these lines from `application.yml` since auth now uses the database:

```yaml
  security:
    user:
      name: admin
      password: admin
```

### Step 9 - Verify

```bash
# Login with the seeded admin user (password: admin123)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@test.com", "password": "admin123"}'
# Returns: {"token": "eyJhbG..."}

# Use the token
curl http://localhost:8080/api/rooms \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkB0ZXN0LmNvbSIsImlhdCI6MTc3MjY1NTU5MCwiZXhwIjoxNzcyNzQxOTkwfQ.JzUQ0jDhbSpm8LNc2ighBBGZbjyn6xqupVaNkhTtjz4"
```

Note: The seeded admin user in `V4__seed_admin_user.sql` has a BCrypt hash of `admin123`. Verify this matches by checking that `BCrypt.matches("admin123", "$2a$10$gVQGfWq0rk2s54lnLxfDTu56633f34dOmk0eJ3VC4oUi6CcnI4C.W")` returns true. If it does not, create a new migration `V5__update_admin_password.sql` with a hash you generate yourself using an online BCrypt generator or a small Java main method.

### Security Consideration

Never commit real secrets to version control. For production, load the JWT secret from an environment variable:

```yaml
jwt:
  secret: ${JWT_SECRET}
```

### Git Checkpoint

```
git add -A
git commit -m "feat: implement JWT authentication with database-backed user login"
```

---

## Phase 13 - Role-Based Authorization

### What You Will Learn

- How Spring Security method-level security works
- How `@PreAuthorize` restricts access based on roles
- How to design authorization rules for a multi-role system

### Authorization Rules

| Operation | ADMIN | USER |
|-----------|-------|------|
| Create room | Yes | No |
| Update/delete room | Yes | No |
| View rooms | Yes | Yes |
| Create booking | Yes | Yes |
| View own bookings | Yes | Yes |
| View all bookings | Yes | No |
| Cancel own booking | Yes | Yes |
| Cancel any booking | Yes | No |
| Create user | Yes | No |
| View all users | Yes | No |

### Step 1 - Enable Method Security

Add `@EnableMethodSecurity` to `SecurityConfig`. This single annotation activates `@PreAuthorize` and `@PostAuthorize` across all controllers:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig
{
    // ... rest of the class remains unchanged from Phase 12
}
```

### Step 2 - Add @PreAuthorize to Controllers

Apply authorization rules to every endpoint. Here are all three controllers with the complete annotations:

Complete `src/main/java/com/booking_api/controller/RoomController.java` (with authorization):

```java
package com.booking_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking_api.dto.RoomRequest;
import com.booking_api.dto.RoomResponse;
import com.booking_api.service.RoomService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/rooms")
public class RoomController
{
    private final RoomService roomService;

    public RoomController(RoomService roomService)
    {
        this.roomService = roomService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping
    public ResponseEntity<List<RoomResponse>> getAllRooms()
    {
        return ResponseEntity.ok(roomService.getAllRooms());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable Long id)
    {
        return ResponseEntity.ok(roomService.getRoomById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody RoomRequest request)
    {
        RoomResponse response = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<RoomResponse> updateRoom(@PathVariable Long id,
                                                   @Valid @RequestBody RoomRequest request)
    {
        return ResponseEntity.ok(roomService.updateRoom(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id)
    {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }
}
```

Complete `src/main/java/com/booking_api/controller/UserController.java` (with authorization):

```java
package com.booking_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking_api.dto.UserRequest;
import com.booking_api.dto.UserResponse;
import com.booking_api.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController
{
    private final UserService userService;

    public UserController(UserService userService)
    {
        this.userService = userService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers()
    {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id)
    {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request)
    {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                                   @Valid @RequestBody UserRequest request)
    {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id)
    {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
```

Complete `src/main/java/com/booking_api/controller/BookingController.java` (with authorization and owner-based access):

```java
package com.booking_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking_api.dto.BookingRequest;
import com.booking_api.dto.BookingResponse;
import com.booking_api.service.BookingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bookings")
public class BookingController
{
    private final BookingService bookingService;

    public BookingController(BookingService bookingService)
    {
        this.bookingService = bookingService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<BookingResponse>> getAllBookings()
    {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/my")
    public ResponseEntity<List<BookingResponse>> getMyBookings(Authentication authentication)
    {
        String email = authentication.getName();
        List<BookingResponse> bookings = bookingService.getBookingsByUserEmail(email);
        return ResponseEntity.ok(bookings);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id)
    {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request)
    {
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PutMapping("/{id}")
    public ResponseEntity<BookingResponse> updateBooking(@PathVariable Long id,
                                                         @Valid @RequestBody BookingRequest request)
    {
        return ResponseEntity.ok(bookingService.updateBooking(id, request));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long id)
    {
        bookingService.cancelBooking(id);
        return ResponseEntity.noContent().build();
    }
}
```

The `getMyBookings` endpoint is the key addition: Spring injects the `Authentication` object automatically when you declare it as a method parameter. `authentication.getName()` returns the username (email) from the JWT.

### Step 3 - Implement Owner-Based Access Control

Add the repository and service methods that support the `/my` endpoint.

Add to `src/main/java/com/booking_api/repository/BookingRepository.java`:

```java
List<Booking> findByUserEmail(String email);
```

Spring Data JPA derives the query automatically from the method name: `findBy` + `User` (navigate the `user` relationship) + `Email` (match the `email` field). No `@Query` annotation needed.

Add to `BookingService`:

```java
public List<BookingResponse> getBookingsByUserEmail(String email)
{
    return bookingRepository.findByUserEmail(email).stream()
        .map(this::toResponse)
        .toList();
}
```

For cancel-own-booking logic, the `cancelBooking` method already works for any user. To restrict users to only cancelling their own bookings, add ownership verification:

```java
public void cancelBooking(Long id, String currentUserEmail)
{
    Booking existing = bookingRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

    if (!existing.getUser().getEmail().equals(currentUserEmail))
    {
        throw new IllegalStateException("You can only cancel your own bookings");
    }

    existing.setStatus(Status.CANCELLED);
    bookingRepository.save(existing);
}
```

Then update the controller to pass the authenticated user's email:

```java
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
@DeleteMapping("/{id}")
public ResponseEntity<Void> cancelBooking(@PathVariable Long id, Authentication authentication)
{
    bookingService.cancelBooking(id, authentication.getName());
    return ResponseEntity.noContent().build();
}
```

For ADMIN users who should be able to cancel any booking, add a separate admin endpoint or check the role in the service:

```java
public void adminCancelBooking(Long id)
{
    Booking existing = bookingRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

    existing.setStatus(Status.CANCELLED);
    bookingRepository.save(existing);
}
```

### Step 4 - Handle 403 Forbidden in GlobalExceptionHandler

Add this handler to `GlobalExceptionHandler.java`:

```java
import org.springframework.security.access.AccessDeniedException;

@ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex)
{
    ErrorResponse error = new ErrorResponse(
        HttpStatus.FORBIDDEN.value(),
        "Access denied",
        LocalDateTime.now()
    );
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
}
```

Without this handler, Spring Security returns a generic 403 HTML page. With it, your API consistently returns JSON error responses.

### Git Checkpoint

```
git add -A
git commit -m "feat: add role-based authorization with method-level security"
```

---

## Phase 14 - Booking Conflict Detection

### What You Will Learn

- How to implement business rule validation at the service layer
- How overlapping time range queries work in SQL
- How to provide meaningful error messages for domain rule violations

### Conflict Rule

Two bookings conflict when they overlap in time for the same room. The existing JPQL query in `BookingRepository` already handles this correctly:

```sql
SELECT b FROM Booking b
WHERE b.room.id = :roomId
AND b.startTime < :endTime
AND b.endTime > :startTime
```

This query finds any booking where the time range overlaps with the requested range.

### Step 1 - Review Existing Validation

`BookingService.createBooking` already validates:
- Start time must be before end time
- Start time must be in the future
- User and room must exist
- No conflicting bookings exist

This logic is correct. Your task is to extend it.

### Step 2 - Add Conflict Detection to Update and Status-Aware Queries

When updating a booking, you need a conflict query that excludes the booking being updated (otherwise it conflicts with itself). Cancelled bookings should also never block new ones.

Here is the complete, final version of `BookingRepository` with both queries:

Complete `src/main/java/com/booking_api/repository/BookingRepository.java`:

```java
package com.booking_api.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.booking_api.model.Booking;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>
{
    @Query("""
        SELECT b FROM Booking b
        WHERE b.room.id = :roomId
        AND b.startTime < :endTime
        AND b.endTime > :startTime
        AND b.status <> com.booking_api.model.Status.CANCELLED
        """)
    List<Booking> findConflictingBookings(
        @Param("roomId") Long roomId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Query("""
        SELECT b FROM Booking b
        WHERE b.room.id = :roomId
        AND b.id <> :excludeBookingId
        AND b.startTime < :endTime
        AND b.endTime > :startTime
        AND b.status <> com.booking_api.model.Status.CANCELLED
        """)
    List<Booking> findConflictingBookingsExcluding(
        @Param("roomId") Long roomId,
        @Param("excludeBookingId") Long excludeBookingId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    List<Booking> findByUserEmail(String email);
}
```

Both queries now include `AND b.status <> com.booking_api.model.Status.CANCELLED`. In JPQL, enum comparison uses the fully qualified class name. This ensures that if a user cancels a booking for 10:00-11:00, another user can immediately book that same slot.

Now update `BookingService.updateBooking` to use the excluding query:

```java
public BookingResponse updateBooking(Long id, BookingRequest request)
{
    Booking existing = bookingRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

    validateTimeRange(request.startTime(), request.endTime());

    Room room = roomRepository.findById(request.roomId())
        .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + request.roomId()));

    List<Booking> conflicts = bookingRepository.findConflictingBookingsExcluding(
        room.getId(), id, request.startTime(), request.endTime());

    if (!conflicts.isEmpty())
    {
        throw new BookingConflictException("Room is already booked during this time");
    }

    existing.setRoom(room);
    existing.setStartTime(request.startTime());
    existing.setEndTime(request.endTime());

    Booking saved = bookingRepository.save(existing);
    return toResponse(saved);
}
```

The difference from the earlier version: instead of querying all conflicts and then filtering with `removeIf`, this version passes the booking ID directly to the database query. This is more efficient because the database filters the row rather than loading it into memory and then discarding it.

### Step 4 - Test Conflict Detection

```bash
# Create a booking
curl -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "roomId": 1, "startTime": "2026-04-01T10:00:00", "endTime": "2026-04-01T11:00:00"}'

# Try to create an overlapping booking for the same room (should return 409)
curl -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "roomId": 1, "startTime": "2026-04-01T10:30:00", "endTime": "2026-04-01T11:30:00"}'
```

### Git Checkpoint

```
git add -A
git commit -m "feat: complete booking conflict detection with status-aware queries"
```

---

## Phase 15 - Testing

### What You Will Learn

- Unit testing with JUnit 5 and Mockito
- Integration testing with `@SpringBootTest`
- Testing Spring Security protected endpoints
- The test pyramid: what to unit test vs integration test
- Using test factories to eliminate duplicated setup code
- Shared base classes for integration tests

### Test Strategy

| Layer | Test Type | What to Verify |
|-------|-----------|----------------|
| Service | Unit test (Mockito) | Business logic, validation rules, conflict detection |
| Repository | Integration test (`@DataJpaTest`) | Custom JPQL queries return correct results |
| Controller | Integration test (`@SpringBootTest`) | HTTP status codes, request/response mapping, security |

Every service gets a unit test class. Every controller gets an integration test class. The `BookingRepository` gets its own integration test because it has custom JPQL queries. `RoomRepository` and `UserRepository` only use standard `JpaRepository` methods, so they are implicitly tested through the service and controller tests.

### Step 1 - Create TestDataFactory

Every test needs `User`, `Room`, `Booking`, and DTO objects. Without a factory, you end up writing `new User(1L, "test@test.com", "password", Role.USER)` dozens of times across test classes. When you add a field to an entity, you have to update every test that constructs one.

A test factory centralizes object creation into static methods. Each test calls the factory instead of constructing objects inline.

Complete `src/test/java/com/booking_api/testutil/TestDataFactory.java`:

```java
package com.booking_api.testutil;

import java.time.LocalDateTime;

import com.booking_api.dto.BookingRequest;
import com.booking_api.dto.RoomRequest;
import com.booking_api.dto.UserRequest;
import com.booking_api.model.Booking;
import com.booking_api.model.Role;
import com.booking_api.model.Room;
import com.booking_api.model.Status;
import com.booking_api.model.User;

public final class TestDataFactory
{
    private TestDataFactory() {}

    public static User createUser(Long id, String email, Role role)
    {
        return new User(id, email, "password", role);
    }

    public static User createDefaultUser()
    {
        return createUser(1L, "test@test.com", Role.USER);
    }

    public static User createAdminUser()
    {
        return createUser(1L, "admin@test.com", Role.ADMIN);
    }

    public static Room createRoom(Long id, String name, int capacity)
    {
        return new Room(id, name, capacity);
    }

    public static Room createDefaultRoom()
    {
        return createRoom(1L, "Room A", 10);
    }

    public static Booking createBooking(Long id, User user, Room room,
                                        LocalDateTime start, LocalDateTime end,
                                        Status status)
    {
        return new Booking(id, user, room, start, end, status);
    }

    public static Booking createConfirmedBooking(Long id, User user, Room room,
                                                 LocalDateTime start, LocalDateTime end)
    {
        return createBooking(id, user, room, start, end, Status.CONFIRMED);
    }

    public static BookingRequest createBookingRequest(Long userId, Long roomId,
                                                      int daysFromNow)
    {
        LocalDateTime start = LocalDateTime.now().plusDays(daysFromNow);
        LocalDateTime end = start.plusHours(1);
        return new BookingRequest(userId, roomId, start, end);
    }

    public static RoomRequest createRoomRequest(String name, int capacity)
    {
        return new RoomRequest(name, capacity);
    }

    public static UserRequest createUserRequest(String email, String password,
                                                String role)
    {
        return new UserRequest(email, password, role);
    }
}
```

Key design decisions:
- The class is `final` with a private constructor so it cannot be instantiated or subclassed. It is a pure utility class.
- `createDefaultUser()` and `createDefaultRoom()` provide single-call convenience for the most common test setup.
- `createBookingRequest` accepts `daysFromNow` instead of raw `LocalDateTime` values, so tests never accidentally create bookings in the past.

### Step 2 - Create IntegrationTestBase

Controller integration tests all need the same boilerplate: start the app on a random port, inject `TestRestTemplate`, log in to get a token, and build auth headers. Extracting this into a base class means each controller test class inherits the shared logic and only contains its own test methods.

Complete `src/test/java/com/booking_api/testutil/IntegrationTestBase.java`:

```java
package com.booking_api.testutil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestBase
{
    @Autowired
    protected TestRestTemplate restTemplate;

    protected String loginAndGetToken(String email, String password)
    {
        record LoginRequest(String email, String password) {}
        record LoginResponse(String token) {}

        LoginRequest request = new LoginRequest(email, password);
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
            "/api/auth/login", request, LoginResponse.class);

        assertNotNull(response.getBody(), "Login response body was null");
        return response.getBody().token();
    }

    protected String loginAsAdmin()
    {
        return loginAndGetToken("admin@test.com", "admin123");
    }

    protected HttpHeaders authHeaders(String token)
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    protected HttpHeaders adminHeaders()
    {
        return authHeaders(loginAsAdmin());
    }
}
```

Key design decisions:
- `loginAsAdmin()` and `adminHeaders()` are one-call shortcuts used in most tests since the seeded admin user is the primary test actor.
- The class is `abstract` so JUnit does not try to run it as a test class.
- `@SpringBootTest` on the base class is inherited by subclasses. You do not need to repeat it.

### Step 3 - Unit Tests for All Services

Each service gets its own test class using Mockito. The mocks replace real repositories so the tests run in milliseconds without a database.

#### BookingServiceTest

Complete `src/test/java/com/booking_api/service/BookingServiceTest.java`:

```java
package com.booking_api.service;

import com.booking_api.dto.BookingRequest;
import com.booking_api.dto.BookingResponse;
import com.booking_api.exception.BookingConflictException;
import com.booking_api.exception.ResourceNotFoundException;
import com.booking_api.model.Booking;
import com.booking_api.model.Room;
import com.booking_api.model.Status;
import com.booking_api.model.User;
import com.booking_api.repository.BookingRepository;
import com.booking_api.repository.RoomRepository;
import com.booking_api.repository.UserRepository;
import com.booking_api.testutil.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest
{
    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    // --- getAllBookings ---

    @Test
    void getAllBookings_shouldReturnList()
    {
        User user = TestDataFactory.createDefaultUser();
        Room room = TestDataFactory.createDefaultRoom();
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        Booking booking = TestDataFactory.createConfirmedBooking(
            1L, user, room, start, start.plusHours(1));

        when(bookingRepository.findAll()).thenReturn(List.of(booking));

        List<BookingResponse> result = bookingService.getAllBookings();

        assertEquals(1, result.size());
        assertEquals("test@test.com", result.get(0).userEmail());
        assertEquals("Room A", result.get(0).roomName());
    }

    @Test
    void getAllBookings_shouldReturnEmptyList()
    {
        when(bookingRepository.findAll()).thenReturn(Collections.emptyList());

        List<BookingResponse> result = bookingService.getAllBookings();

        assertTrue(result.isEmpty());
    }

    // --- getBookingsByUserEmail ---

    @Test
    void getBookingsByUserEmail_shouldReturnUserBookings()
    {
        User user = TestDataFactory.createDefaultUser();
        Room room = TestDataFactory.createDefaultRoom();
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        Booking booking = TestDataFactory.createConfirmedBooking(
            1L, user, room, start, start.plusHours(1));

        when(bookingRepository.findByUserEmail("test@test.com"))
            .thenReturn(List.of(booking));

        List<BookingResponse> result = bookingService.getBookingsByUserEmail("test@test.com");

        assertEquals(1, result.size());
        assertEquals("test@test.com", result.get(0).userEmail());
    }

    // --- getBookingById ---

    @Test
    void getBookingById_shouldReturnBooking()
    {
        User user = TestDataFactory.createDefaultUser();
        Room room = TestDataFactory.createDefaultRoom();
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        Booking booking = TestDataFactory.createConfirmedBooking(
            1L, user, room, start, start.plusHours(1));

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        BookingResponse result = bookingService.getBookingById(1L);

        assertEquals(1L, result.id());
        assertEquals("test@test.com", result.userEmail());
        assertEquals("CONFIRMED", result.status());
    }

    @Test
    void getBookingById_shouldThrowWhenNotFound()
    {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> bookingService.getBookingById(99L));
    }

    // --- createBooking ---

    @Test
    void createBooking_shouldSaveWhenNoConflict()
    {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(1);
        BookingRequest request = new BookingRequest(1L, 1L, start, end);

        User user = TestDataFactory.createDefaultUser();
        Room room = TestDataFactory.createDefaultRoom();
        Booking saved = TestDataFactory.createConfirmedBooking(1L, user, room, start, end);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(bookingRepository.findConflictingBookings(eq(1L), any(), any()))
            .thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenReturn(saved);

        BookingResponse response = bookingService.createBooking(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("test@test.com", response.userEmail());
        assertEquals("Room A", response.roomName());
        assertEquals("CONFIRMED", response.status());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void createBooking_shouldRejectPastStartTime()
    {
        BookingRequest request = new BookingRequest(
            1L, 1L,
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(1)
        );

        assertThrows(IllegalArgumentException.class,
            () -> bookingService.createBooking(request));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_shouldRejectInvalidTimeRange()
    {
        LocalDateTime future = LocalDateTime.now().plusDays(1);

        BookingRequest request = new BookingRequest(
            1L, 1L,
            future.plusHours(2),
            future
        );

        assertThrows(IllegalArgumentException.class,
            () -> bookingService.createBooking(request));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_shouldThrowWhenUserNotFound()
    {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        BookingRequest request = new BookingRequest(99L, 1L, start, start.plusHours(1));

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> bookingService.createBooking(request));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_shouldThrowWhenRoomNotFound()
    {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        BookingRequest request = new BookingRequest(1L, 99L, start, start.plusHours(1));

        User user = TestDataFactory.createDefaultUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> bookingService.createBooking(request));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_shouldRejectConflictingBooking()
    {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(1);
        BookingRequest request = new BookingRequest(1L, 1L, start, end);

        User user = TestDataFactory.createDefaultUser();
        Room room = TestDataFactory.createDefaultRoom();
        Booking conflict = TestDataFactory.createConfirmedBooking(99L, user, room, start, end);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(bookingRepository.findConflictingBookings(eq(1L), any(), any()))
            .thenReturn(List.of(conflict));

        assertThrows(BookingConflictException.class,
            () -> bookingService.createBooking(request));

        verify(bookingRepository, never()).save(any());
    }

    // --- updateBooking ---

    @Test
    void updateBooking_shouldUpdateSuccessfully()
    {
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        LocalDateTime end = start.plusHours(1);
        BookingRequest request = new BookingRequest(1L, 1L, start, end);

        User user = TestDataFactory.createDefaultUser();
        Room room = TestDataFactory.createDefaultRoom();
        Booking existing = TestDataFactory.createConfirmedBooking(
            1L, user, room, LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(1).plusHours(1));

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(bookingRepository.findConflictingBookingsExcluding(eq(1L), eq(1L), any(), any()))
            .thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenReturn(existing);

        BookingResponse response = bookingService.updateBooking(1L, request);

        assertNotNull(response);
        verify(bookingRepository).save(existing);
    }

    @Test
    void updateBooking_shouldThrowWhenNotFound()
    {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        BookingRequest request = new BookingRequest(1L, 1L, start, start.plusHours(1));

        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> bookingService.updateBooking(99L, request));
    }

    @Test
    void updateBooking_shouldThrowWhenConflicting()
    {
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        LocalDateTime end = start.plusHours(1);
        BookingRequest request = new BookingRequest(1L, 1L, start, end);

        User user = TestDataFactory.createDefaultUser();
        Room room = TestDataFactory.createDefaultRoom();
        Booking existing = TestDataFactory.createConfirmedBooking(
            1L, user, room, LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(1).plusHours(1));
        Booking conflict = TestDataFactory.createConfirmedBooking(99L, user, room, start, end);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(bookingRepository.findConflictingBookingsExcluding(eq(1L), eq(1L), any(), any()))
            .thenReturn(List.of(conflict));

        assertThrows(BookingConflictException.class,
            () -> bookingService.updateBooking(1L, request));

        verify(bookingRepository, never()).save(any());
    }

    // --- cancelBooking ---

    @Test
    void cancelBooking_shouldSetStatusToCancelled()
    {
        User user = TestDataFactory.createDefaultUser();
        Room room = TestDataFactory.createDefaultRoom();
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        Booking booking = TestDataFactory.createConfirmedBooking(
            1L, user, room, start, start.plusHours(1));

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        bookingService.cancelBooking(1L, "test@test.com");

        assertEquals(Status.CANCELLED, booking.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void cancelBooking_shouldRejectWhenNotOwner()
    {
        User user = TestDataFactory.createDefaultUser();
        Room room = TestDataFactory.createDefaultRoom();
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        Booking booking = TestDataFactory.createConfirmedBooking(
            1L, user, room, start, start.plusHours(1));

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(IllegalStateException.class,
            () -> bookingService.cancelBooking(1L, "other@test.com"));

        verify(bookingRepository, never()).save(any());
    }

    // --- adminCancelBooking ---

    @Test
    void adminCancelBooking_shouldSetStatusToCancelled()
    {
        User user = TestDataFactory.createDefaultUser();
        Room room = TestDataFactory.createDefaultRoom();
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        Booking booking = TestDataFactory.createConfirmedBooking(
            1L, user, room, start, start.plusHours(1));

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        bookingService.adminCancelBooking(1L);

        assertEquals(Status.CANCELLED, booking.getStatus());
        verify(bookingRepository).save(booking);
    }
}
```

#### RoomServiceTest

Complete `src/test/java/com/booking_api/service/RoomServiceTest.java`:

```java
package com.booking_api.service;

import com.booking_api.dto.RoomRequest;
import com.booking_api.dto.RoomResponse;
import com.booking_api.exception.ResourceNotFoundException;
import com.booking_api.model.Room;
import com.booking_api.repository.RoomRepository;
import com.booking_api.testutil.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest
{
    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomService roomService;

    // --- getAllRooms ---

    @Test
    void getAllRooms_shouldReturnList()
    {
        Room room = TestDataFactory.createDefaultRoom();
        when(roomRepository.findAll()).thenReturn(List.of(room));

        List<RoomResponse> result = roomService.getAllRooms();

        assertEquals(1, result.size());
        assertEquals("Room A", result.get(0).name());
    }

    // --- getRoomById ---

    @Test
    void getRoomById_shouldReturnRoom()
    {
        Room room = TestDataFactory.createDefaultRoom();
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        RoomResponse result = roomService.getRoomById(1L);

        assertEquals(1L, result.id());
        assertEquals("Room A", result.name());
        assertEquals(10, result.capacity());
    }

    @Test
    void getRoomById_shouldThrowWhenNotFound()
    {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> roomService.getRoomById(99L));
    }

    // --- createRoom ---

    @Test
    void createRoom_shouldSaveAndReturnResponse()
    {
        RoomRequest request = TestDataFactory.createRoomRequest("New Room", 15);
        Room saved = TestDataFactory.createRoom(1L, "New Room", 15);
        when(roomRepository.save(any(Room.class))).thenReturn(saved);

        RoomResponse result = roomService.createRoom(request);

        assertEquals("New Room", result.name());
        assertEquals(15, result.capacity());
        verify(roomRepository).save(any(Room.class));
    }

    // --- updateRoom ---

    @Test
    void updateRoom_shouldUpdateAndReturnResponse()
    {
        Room existing = TestDataFactory.createRoom(1L, "Old Room", 10);
        Room updated = TestDataFactory.createRoom(1L, "Updated Room", 20);

        when(roomRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(roomRepository.save(any(Room.class))).thenReturn(updated);

        RoomRequest request = TestDataFactory.createRoomRequest("Updated Room", 20);
        RoomResponse result = roomService.updateRoom(1L, request);

        assertEquals("Updated Room", result.name());
        assertEquals(20, result.capacity());
        verify(roomRepository).save(existing);
    }

    @Test
    void updateRoom_shouldThrowWhenNotFound()
    {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        RoomRequest request = TestDataFactory.createRoomRequest("Any", 5);

        assertThrows(ResourceNotFoundException.class,
            () -> roomService.updateRoom(99L, request));

        verify(roomRepository, never()).save(any());
    }

    // --- deleteRoom ---

    @Test
    void deleteRoom_shouldDeleteWhenExists()
    {
        when(roomRepository.existsById(1L)).thenReturn(true);

        roomService.deleteRoom(1L);

        verify(roomRepository).deleteById(1L);
    }

    @Test
    void deleteRoom_shouldThrowWhenNotFound()
    {
        when(roomRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
            () -> roomService.deleteRoom(99L));

        verify(roomRepository, never()).deleteById(any());
    }
}
```

#### UserServiceTest

Complete `src/test/java/com/booking_api/service/UserServiceTest.java`:

```java
package com.booking_api.service;

import com.booking_api.dto.UserRequest;
import com.booking_api.dto.UserResponse;
import com.booking_api.exception.ResourceNotFoundException;
import com.booking_api.model.Role;
import com.booking_api.model.User;
import com.booking_api.repository.UserRepository;
import com.booking_api.testutil.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest
{
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    // --- getAllUsers ---

    @Test
    void getAllUsers_shouldReturnList()
    {
        User user = TestDataFactory.createDefaultUser();
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserResponse> result = userService.getAllUsers();

        assertEquals(1, result.size());
        assertEquals("test@test.com", result.get(0).email());
    }

    // --- getUserById ---

    @Test
    void getUserById_shouldReturnUser()
    {
        User user = TestDataFactory.createDefaultUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse result = userService.getUserById(1L);

        assertEquals(1L, result.id());
        assertEquals("test@test.com", result.email());
        assertEquals("USER", result.role());
    }

    @Test
    void getUserById_shouldThrowWhenNotFound()
    {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> userService.getUserById(99L));
    }

    // --- createUser ---

    @Test
    void createUser_shouldDefaultToUserRole()
    {
        UserRequest request = TestDataFactory.createUserRequest(
            "new@test.com", "password123", null);
        User saved = TestDataFactory.createUser(1L, "new@test.com", Role.USER);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserResponse result = userService.createUser(request);

        assertEquals("USER", result.role());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_shouldParseAdminRole()
    {
        UserRequest request = TestDataFactory.createUserRequest(
            "admin@new.com", "password123", "ADMIN");
        User saved = TestDataFactory.createUser(1L, "admin@new.com", Role.ADMIN);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserResponse result = userService.createUser(request);

        assertEquals("ADMIN", result.role());
    }

    // --- updateUser ---

    @Test
    void updateUser_shouldUpdateAndReturnResponse()
    {
        User existing = TestDataFactory.createUser(1L, "old@test.com", Role.USER);
        User updated = TestDataFactory.createUser(1L, "updated@test.com", Role.ADMIN);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenReturn(updated);

        UserRequest request = TestDataFactory.createUserRequest(
            "updated@test.com", "newpass", "ADMIN");
        UserResponse result = userService.updateUser(1L, request);

        assertEquals("updated@test.com", result.email());
        assertEquals("ADMIN", result.role());
        verify(userRepository).save(existing);
    }

    @Test
    void updateUser_shouldThrowWhenNotFound()
    {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        UserRequest request = TestDataFactory.createUserRequest(
            "any@test.com", "password", "USER");

        assertThrows(ResourceNotFoundException.class,
            () -> userService.updateUser(99L, request));

        verify(userRepository, never()).save(any());
    }

    // --- deleteUser ---

    @Test
    void deleteUser_shouldDeleteWhenExists()
    {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_shouldThrowWhenNotFound()
    {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
            () -> userService.deleteUser(99L));

        verify(userRepository, never()).deleteById(any());
    }
}
```

### Step 4 - Repository Integration Test

The `BookingRepository` has custom JPQL queries that need to be tested against the real database. `RoomRepository` and `UserRepository` only use standard `JpaRepository` methods (`findAll`, `findById`, `save`, `deleteById`), so they do not need their own test classes -- their correctness is verified by Spring Data JPA itself and by the controller integration tests.

Complete `src/test/java/com/booking_api/repository/BookingRepositoryTest.java`:

```java
package com.booking_api.repository;

import com.booking_api.model.Booking;
import com.booking_api.model.Role;
import com.booking_api.model.Room;
import com.booking_api.model.Status;
import com.booking_api.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookingRepositoryTest
{
    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    private Room room;
    private User user;

    @BeforeEach
    void setUp()
    {
        bookingRepository.deleteAll();
        roomRepository.deleteAll();
        userRepository.deleteAll();

        user = new User();
        user.setEmail("test@test.com");
        user.setPassword("password");
        user.setRole(Role.USER);
        user = userRepository.save(user);

        room = new Room();
        room.setName("Test Room");
        room.setCapacity(10);
        room = roomRepository.save(room);
    }

    // --- findConflictingBookings ---

    @Test
    void findConflictingBookings_shouldDetectOverlap()
    {
        LocalDateTime tenAM = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime elevenAM = LocalDateTime.of(2026, 6, 1, 11, 0);

        Booking existing = new Booking();
        existing.setUser(user);
        existing.setRoom(room);
        existing.setStartTime(tenAM);
        existing.setEndTime(elevenAM);
        existing.setStatus(Status.CONFIRMED);
        bookingRepository.save(existing);

        List<Booking> conflicts = bookingRepository.findConflictingBookings(
            room.getId(), tenAM.plusMinutes(30), elevenAM.plusMinutes(30));

        assertFalse(conflicts.isEmpty());
        assertEquals(1, conflicts.size());
    }

    @Test
    void findConflictingBookings_shouldNotDetectNonOverlap()
    {
        LocalDateTime tenAM = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime elevenAM = LocalDateTime.of(2026, 6, 1, 11, 0);

        Booking existing = new Booking();
        existing.setUser(user);
        existing.setRoom(room);
        existing.setStartTime(tenAM);
        existing.setEndTime(elevenAM);
        existing.setStatus(Status.CONFIRMED);
        bookingRepository.save(existing);

        List<Booking> conflicts = bookingRepository.findConflictingBookings(
            room.getId(), elevenAM, elevenAM.plusHours(1));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void findConflictingBookings_shouldIgnoreCancelledBookings()
    {
        LocalDateTime tenAM = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime elevenAM = LocalDateTime.of(2026, 6, 1, 11, 0);

        Booking cancelled = new Booking();
        cancelled.setUser(user);
        cancelled.setRoom(room);
        cancelled.setStartTime(tenAM);
        cancelled.setEndTime(elevenAM);
        cancelled.setStatus(Status.CANCELLED);
        bookingRepository.save(cancelled);

        List<Booking> conflicts = bookingRepository.findConflictingBookings(
            room.getId(), tenAM, elevenAM);

        assertTrue(conflicts.isEmpty());
    }

    // --- findConflictingBookingsExcluding ---

    @Test
    void findConflictingBookingsExcluding_shouldExcludeOwnBooking()
    {
        LocalDateTime tenAM = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime elevenAM = LocalDateTime.of(2026, 6, 1, 11, 0);

        Booking own = new Booking();
        own.setUser(user);
        own.setRoom(room);
        own.setStartTime(tenAM);
        own.setEndTime(elevenAM);
        own.setStatus(Status.CONFIRMED);
        own = bookingRepository.save(own);

        List<Booking> conflicts = bookingRepository.findConflictingBookingsExcluding(
            room.getId(), own.getId(), tenAM, elevenAM);

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void findConflictingBookingsExcluding_shouldDetectOtherConflicts()
    {
        LocalDateTime tenAM = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime elevenAM = LocalDateTime.of(2026, 6, 1, 11, 0);

        Booking own = new Booking();
        own.setUser(user);
        own.setRoom(room);
        own.setStartTime(tenAM.plusHours(2));
        own.setEndTime(elevenAM.plusHours(2));
        own.setStatus(Status.CONFIRMED);
        own = bookingRepository.save(own);

        Booking other = new Booking();
        other.setUser(user);
        other.setRoom(room);
        other.setStartTime(tenAM);
        other.setEndTime(elevenAM);
        other.setStatus(Status.CONFIRMED);
        bookingRepository.save(other);

        List<Booking> conflicts = bookingRepository.findConflictingBookingsExcluding(
            room.getId(), own.getId(), tenAM.plusMinutes(30), elevenAM.plusMinutes(30));

        assertEquals(1, conflicts.size());
    }

    // --- findByUserEmail ---

    @Test
    void findByUserEmail_shouldReturnUserBookings()
    {
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoom(room);
        booking.setStartTime(LocalDateTime.of(2026, 6, 1, 10, 0));
        booking.setEndTime(LocalDateTime.of(2026, 6, 1, 11, 0));
        booking.setStatus(Status.CONFIRMED);
        bookingRepository.save(booking);

        List<Booking> results = bookingRepository.findByUserEmail("test@test.com");

        assertEquals(1, results.size());
        assertEquals("test@test.com", results.get(0).getUser().getEmail());
    }
}
```

Note: `BookingRepositoryTest` does not use `TestDataFactory` because `@DataJpaTest` manages entities through JPA, and saved entities need auto-generated IDs from the database. The factory's ID-bearing constructors would conflict with `@GeneratedValue`. The `@BeforeEach` setup uses setters to let JPA assign IDs.

### Step 5 - Controller Integration Tests for All Controllers

Each controller gets a test class that extends `IntegrationTestBase`. Tests verify the full HTTP cycle: request serialization, JWT authentication, `@PreAuthorize` authorization, `@Valid` input validation, service execution, and response mapping.

#### RoomControllerTest

Complete `src/test/java/com/booking_api/controller/RoomControllerTest.java`:

```java
package com.booking_api.controller;

import com.booking_api.dto.RoomRequest;
import com.booking_api.dto.RoomResponse;
import com.booking_api.testutil.IntegrationTestBase;
import com.booking_api.testutil.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class RoomControllerTest extends IntegrationTestBase
{
    // --- security ---

    @Test
    void getRooms_withoutAuth_shouldReturn401()
    {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/rooms", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void createRoom_withoutAuth_shouldReturn401()
    {
        RoomRequest request = TestDataFactory.createRoomRequest("Test Room", 10);
        HttpEntity<RoomRequest> entity = new HttpEntity<>(request);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/rooms", HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // --- GET /api/rooms ---

    @Test
    void getRooms_asAdmin_shouldReturn200()
    {
        HttpEntity<Void> entity = new HttpEntity<>(adminHeaders());

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/rooms", HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- GET /api/rooms/{id} ---

    @Test
    void getRoomById_asAdmin_shouldReturn200()
    {
        HttpHeaders headers = adminHeaders();

        RoomRequest createRequest = TestDataFactory.createRoomRequest("Lookup Room", 8);
        HttpEntity<RoomRequest> createEntity = new HttpEntity<>(createRequest, headers);
        ResponseEntity<RoomResponse> createResponse = restTemplate.exchange(
            "/api/rooms", HttpMethod.POST, createEntity, RoomResponse.class);
        Long roomId = createResponse.getBody().id();

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<RoomResponse> response = restTemplate.exchange(
            "/api/rooms/" + roomId, HttpMethod.GET, entity, RoomResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Lookup Room", response.getBody().name());
    }

    // --- POST /api/rooms ---

    @Test
    void createRoom_asAdmin_shouldReturn201()
    {
        RoomRequest request = TestDataFactory.createRoomRequest("Integration Room", 5);
        HttpEntity<RoomRequest> entity = new HttpEntity<>(request, adminHeaders());

        ResponseEntity<RoomResponse> response = restTemplate.exchange(
            "/api/rooms", HttpMethod.POST, entity, RoomResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Integration Room", response.getBody().name());
        assertEquals(5, response.getBody().capacity());
    }

    @Test
    void createRoom_withInvalidData_shouldReturn400()
    {
        RoomRequest request = TestDataFactory.createRoomRequest("", -1);
        HttpEntity<RoomRequest> entity = new HttpEntity<>(request, adminHeaders());

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/rooms", HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // --- PUT /api/rooms/{id} ---

    @Test
    void updateRoom_asAdmin_shouldReturn200()
    {
        HttpHeaders headers = adminHeaders();

        RoomRequest createRequest = TestDataFactory.createRoomRequest("Before Update", 4);
        HttpEntity<RoomRequest> createEntity = new HttpEntity<>(createRequest, headers);
        ResponseEntity<RoomResponse> createResponse = restTemplate.exchange(
            "/api/rooms", HttpMethod.POST, createEntity, RoomResponse.class);
        Long roomId = createResponse.getBody().id();

        RoomRequest updateRequest = TestDataFactory.createRoomRequest("After Update", 12);
        HttpEntity<RoomRequest> updateEntity = new HttpEntity<>(updateRequest, headers);
        ResponseEntity<RoomResponse> response = restTemplate.exchange(
            "/api/rooms/" + roomId, HttpMethod.PUT, updateEntity, RoomResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("After Update", response.getBody().name());
        assertEquals(12, response.getBody().capacity());
    }

    // --- DELETE /api/rooms/{id} ---

    @Test
    void deleteRoom_asAdmin_shouldReturn204()
    {
        HttpHeaders headers = adminHeaders();

        RoomRequest createRequest = TestDataFactory.createRoomRequest("To Delete", 3);
        HttpEntity<RoomRequest> createEntity = new HttpEntity<>(createRequest, headers);
        ResponseEntity<RoomResponse> createResponse = restTemplate.exchange(
            "/api/rooms", HttpMethod.POST, createEntity, RoomResponse.class);
        Long roomId = createResponse.getBody().id();

        HttpEntity<Void> deleteEntity = new HttpEntity<>(headers);
        ResponseEntity<Void> response = restTemplate.exchange(
            "/api/rooms/" + roomId, HttpMethod.DELETE, deleteEntity, Void.class);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
```

#### BookingControllerTest

Complete `src/test/java/com/booking_api/controller/BookingControllerTest.java`:

```java
package com.booking_api.controller;

import com.booking_api.dto.BookingRequest;
import com.booking_api.dto.BookingResponse;
import com.booking_api.dto.RoomRequest;
import com.booking_api.dto.RoomResponse;
import com.booking_api.testutil.IntegrationTestBase;
import com.booking_api.testutil.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BookingControllerTest extends IntegrationTestBase
{
    private HttpHeaders headers;
    private Long roomId;

    @BeforeEach
    void setUp()
    {
        headers = adminHeaders();

        RoomRequest roomRequest = TestDataFactory.createRoomRequest("Booking Test Room", 10);
        HttpEntity<RoomRequest> roomEntity = new HttpEntity<>(roomRequest, headers);
        ResponseEntity<RoomResponse> roomResponse = restTemplate.exchange(
            "/api/rooms", HttpMethod.POST, roomEntity, RoomResponse.class);

        assertNotNull(roomResponse.getBody());
        roomId = roomResponse.getBody().id();
    }

    private BookingResponse createTestBooking(int daysFromNow)
    {
        LocalDateTime start = LocalDateTime.now().plusDays(daysFromNow);
        BookingRequest request = new BookingRequest(1L, roomId, start, start.plusHours(1));
        HttpEntity<BookingRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<BookingResponse> response = restTemplate.exchange(
            "/api/bookings", HttpMethod.POST, entity, BookingResponse.class);
        assertNotNull(response.getBody());
        return response.getBody();
    }

    // --- security ---

    @Test
    void createBooking_withoutAuth_shouldReturn401()
    {
        BookingRequest request = new BookingRequest(
            1L, roomId,
            LocalDateTime.now().plusDays(5),
            LocalDateTime.now().plusDays(5).plusHours(1)
        );
        HttpEntity<BookingRequest> entity = new HttpEntity<>(request);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/bookings", HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // --- POST /api/bookings ---

    @Test
    void createBooking_asAdmin_shouldReturn201()
    {
        BookingRequest request = new BookingRequest(
            1L, roomId,
            LocalDateTime.now().plusDays(10),
            LocalDateTime.now().plusDays(10).plusHours(1)
        );
        HttpEntity<BookingRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<BookingResponse> response = restTemplate.exchange(
            "/api/bookings", HttpMethod.POST, entity, BookingResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("CONFIRMED", response.getBody().status());
        assertEquals(roomId, response.getBody().roomId());
    }

    @Test
    void createBooking_conflicting_shouldReturn409()
    {
        LocalDateTime start = LocalDateTime.now().plusDays(20);
        LocalDateTime end = start.plusHours(1);

        BookingRequest first = new BookingRequest(1L, roomId, start, end);
        HttpEntity<BookingRequest> firstEntity = new HttpEntity<>(first, headers);
        restTemplate.exchange("/api/bookings", HttpMethod.POST, firstEntity, BookingResponse.class);

        BookingRequest overlapping = new BookingRequest(
            1L, roomId,
            start.plusMinutes(30),
            end.plusMinutes(30)
        );
        HttpEntity<BookingRequest> secondEntity = new HttpEntity<>(overlapping, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/bookings", HttpMethod.POST, secondEntity, String.class);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    // --- GET /api/bookings ---

    @Test
    void getAllBookings_asAdmin_shouldReturn200()
    {
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/bookings", HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- GET /api/bookings/my ---

    @Test
    void getMyBookings_asAdmin_shouldReturn200()
    {
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/bookings/my", HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- GET /api/bookings/{id} ---

    @Test
    void getBookingById_asAdmin_shouldReturn200()
    {
        BookingResponse created = createTestBooking(30);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<BookingResponse> response = restTemplate.exchange(
            "/api/bookings/" + created.id(), HttpMethod.GET, entity, BookingResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(created.id(), response.getBody().id());
    }

    // --- PUT /api/bookings/{id} ---

    @Test
    void updateBooking_asAdmin_shouldReturn200()
    {
        BookingResponse created = createTestBooking(40);

        LocalDateTime newStart = LocalDateTime.now().plusDays(41);
        BookingRequest updateRequest = new BookingRequest(
            1L, roomId, newStart, newStart.plusHours(2));
        HttpEntity<BookingRequest> entity = new HttpEntity<>(updateRequest, headers);

        ResponseEntity<BookingResponse> response = restTemplate.exchange(
            "/api/bookings/" + created.id(), HttpMethod.PUT, entity, BookingResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- DELETE /api/bookings/{id} ---

    @Test
    void cancelBooking_asAdmin_shouldReturn204()
    {
        BookingResponse created = createTestBooking(50);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Void> response = restTemplate.exchange(
            "/api/bookings/" + created.id(), HttpMethod.DELETE, entity, Void.class);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
```

#### UserControllerTest

Complete `src/test/java/com/booking_api/controller/UserControllerTest.java`:

```java
package com.booking_api.controller;

import com.booking_api.dto.UserRequest;
import com.booking_api.dto.UserResponse;
import com.booking_api.testutil.IntegrationTestBase;
import com.booking_api.testutil.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest extends IntegrationTestBase
{
    private int emailCounter = 0;

    private UserResponse createTestUser()
    {
        emailCounter++;
        HttpHeaders headers = adminHeaders();
        UserRequest request = TestDataFactory.createUserRequest(
            "testuser" + emailCounter + "@test.com", "password123", "USER");
        HttpEntity<UserRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<UserResponse> response = restTemplate.exchange(
            "/api/users", HttpMethod.POST, entity, UserResponse.class);
        assertNotNull(response.getBody());
        return response.getBody();
    }

    // --- security ---

    @Test
    void getAllUsers_withoutAuth_shouldReturn401()
    {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/users", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void createUser_withoutAuth_shouldReturn401()
    {
        UserRequest request = TestDataFactory.createUserRequest(
            "someone@test.com", "password123", "USER");
        HttpEntity<UserRequest> entity = new HttpEntity<>(request);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/users", HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // --- GET /api/users ---

    @Test
    void getAllUsers_asAdmin_shouldReturn200()
    {
        HttpEntity<Void> entity = new HttpEntity<>(adminHeaders());

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/users", HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- GET /api/users/{id} ---

    @Test
    void getUserById_asAdmin_shouldReturn200()
    {
        UserResponse created = createTestUser();

        HttpEntity<Void> entity = new HttpEntity<>(adminHeaders());
        ResponseEntity<UserResponse> response = restTemplate.exchange(
            "/api/users/" + created.id(), HttpMethod.GET, entity, UserResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(created.email(), response.getBody().email());
    }

    // --- POST /api/users ---

    @Test
    void createUser_asAdmin_shouldReturn201()
    {
        UserRequest request = TestDataFactory.createUserRequest(
            "newuser@test.com", "password123", "USER");
        HttpEntity<UserRequest> entity = new HttpEntity<>(request, adminHeaders());

        ResponseEntity<UserResponse> response = restTemplate.exchange(
            "/api/users", HttpMethod.POST, entity, UserResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("newuser@test.com", response.getBody().email());
        assertEquals("USER", response.getBody().role());
    }

    @Test
    void createUser_withInvalidEmail_shouldReturn400()
    {
        UserRequest request = TestDataFactory.createUserRequest(
            "not-an-email", "password123", "USER");
        HttpEntity<UserRequest> entity = new HttpEntity<>(request, adminHeaders());

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/users", HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // --- PUT /api/users/{id} ---

    @Test
    void updateUser_asAdmin_shouldReturn200()
    {
        UserResponse created = createTestUser();

        UserRequest updateRequest = TestDataFactory.createUserRequest(
            "updated@test.com", "newpassword", "ADMIN");
        HttpEntity<UserRequest> entity = new HttpEntity<>(updateRequest, adminHeaders());

        ResponseEntity<UserResponse> response = restTemplate.exchange(
            "/api/users/" + created.id(), HttpMethod.PUT, entity, UserResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("updated@test.com", response.getBody().email());
        assertEquals("ADMIN", response.getBody().role());
    }

    // --- DELETE /api/users/{id} ---

    @Test
    void deleteUser_asAdmin_shouldReturn204()
    {
        UserResponse created = createTestUser();

        HttpEntity<Void> entity = new HttpEntity<>(adminHeaders());
        ResponseEntity<Void> response = restTemplate.exchange(
            "/api/users/" + created.id(), HttpMethod.DELETE, entity, Void.class);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
```

### Test File Summary

| File | Type | Tests | What It Covers |
|------|------|-------|----------------|
| `testutil/TestDataFactory.java` | Utility | -- | Reusable factory methods for test objects |
| `testutil/IntegrationTestBase.java` | Base class | -- | Shared login, auth headers, TestRestTemplate |
| `service/BookingServiceTest.java` | Unit | 17 | All 7 public methods: happy paths, validation, conflict, not-found, ownership |
| `service/RoomServiceTest.java` | Unit | 8 | All 5 public methods: happy paths, not-found for get/update/delete |
| `service/UserServiceTest.java` | Unit | 9 | All 5 public methods: happy paths, not-found, role parsing |
| `repository/BookingRepositoryTest.java` | Integration | 6 | Both JPQL queries including excluding variant, status-aware conflict detection |
| `controller/RoomControllerTest.java` | Integration | 8 | All 5 endpoints: GET, GET/{id}, POST, PUT, DELETE + auth + validation |
| `controller/BookingControllerTest.java` | Integration | 8 | All 6 endpoints: GET, GET/my, GET/{id}, POST, PUT, DELETE + conflict 409 |
| `controller/UserControllerTest.java` | Integration | 8 | All 5 endpoints: GET, GET/{id}, POST, PUT, DELETE + auth + validation |

**Total: 64 tests** across 7 test classes, supported by 2 shared utility classes.

Every public service method now has at least a happy-path test and its primary error-path test. Every controller endpoint has an integration test verifying the HTTP status code and response body. The `findConflictingBookingsExcluding` repository query is tested alongside the original `findConflictingBookings` query.

### Git Checkpoint

```
git add -A
git commit -m "test: add comprehensive unit and integration tests with test factories"
```

---

## Phase 16 - Dockerize the Application

### What You Will Learn

- How to write a multi-stage Dockerfile for a Spring Boot app
- How Docker Compose orchestrates multiple containers
- How containers communicate via Docker networking
- How to configure service dependencies and health checks

### Step 1 - Create a Dockerfile

Create `booking-api/Dockerfile`:

```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B
COPY src/ src/
RUN ./mvnw package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

This is a multi-stage build: the first stage compiles the code, the second stage runs it with a smaller JRE image.

### Step 2 - Update docker-compose.yml

Add the Spring Boot application as a service and configure it to wait for PostgreSQL:

```yaml
services:
  postgres:
    image: postgres:15
    container_name: booking-postgres
    restart: always
    environment:
      POSTGRES_DB: bookingdb
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 5s
      retries: 5

  app:
    build: .
    container_name: booking-app
    restart: always
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/bookingdb
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      JWT_SECRET: ${JWT_SECRET:-default-dev-secret-change-in-production}
    depends_on:
      postgres:
        condition: service_healthy

volumes:
  postgres-data:
```

Key points to understand:
- The app container uses `postgres` (the service name) as the database hostname, not `localhost`
- Environment variables override `application.yml` values
- `depends_on` with `condition: service_healthy` ensures PostgreSQL is ready before the app starts
- The JWT secret is loaded from an environment variable with a dev fallback

### Step 3 - Create a .env File

Create `booking-api/.env` (add to `.gitignore`):

```
JWT_SECRET=your-base64-encoded-secret-for-development
```

### Step 4 - Verify

```bash
docker compose up --build
```

```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

### Git Checkpoint

```
git add -A
git commit -m "feat: dockerize Spring Boot application with multi-stage build"
```

---

## Phase 17 - Production Readiness

### What You Will Learn

- Application profiles for environment-specific configuration
- Structured logging
- Database connection pooling
- API documentation with OpenAPI/Swagger

### Step 1 - Add Spring Profiles

Create `src/main/resources/application-dev.yml`:

```yaml
spring:
  jpa:
    show-sql: true

jwt:
  secret: dev-only-secret-not-for-production
  expiration: 86400000
```

Create `src/main/resources/application-prod.yml`:

```yaml
spring:
  jpa:
    show-sql: false
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

jwt:
  secret: ${JWT_SECRET}
  expiration: 3600000

management:
  endpoints:
    web:
      exposure:
        include: health
```

Activate a profile at runtime:

```bash
java -jar app.jar --spring.profiles.active=prod
```

Or via environment variable in docker-compose:

```yaml
environment:
  SPRING_PROFILES_ACTIVE: prod
```

### Step 2 - Add OpenAPI Documentation

Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.6</version>
</dependency>
```

Permit the Swagger UI path in `SecurityConfig`:

```java
.requestMatchers("/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
```

After restarting, visit `http://localhost:8080/swagger-ui.html` to see auto-generated API documentation.

### Step 3 - Add a User Registration Endpoint

Complete `src/main/java/com/booking_api/dto/RegisterRequest.java`:

```java
package com.booking_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    String password
) {}
```

Add the existence check to `src/main/java/com/booking_api/repository/UserRepository.java`:

```java
package com.booking_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.booking_api.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long>
{
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
```

Add the register endpoint to `AuthController`. Here is the complete controller with both login and register:

Complete `src/main/java/com/booking_api/controller/AuthController.java` (with registration):

```java
package com.booking_api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking_api.dto.AuthRequest;
import com.booking_api.dto.AuthResponse;
import com.booking_api.dto.RegisterRequest;
import com.booking_api.dto.UserResponse;
import com.booking_api.model.Role;
import com.booking_api.model.User;
import com.booking_api.repository.UserRepository;
import com.booking_api.service.CustomUserDetailsService;
import com.booking_api.service.JwtService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController
{
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          CustomUserDetailsService userDetailsService,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder)
    {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request)
    {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String token = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request)
    {
        if (userRepository.existsByEmail(request.email()))
        {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);

        User saved = userRepository.save(user);

        UserResponse response = new UserResponse(saved.getId(), saved.getEmail(), saved.getRole().name());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

Key points:
- `passwordEncoder.encode(request.password())` hashes the plain-text password with BCrypt before saving. The stored value looks like `$2a$10$...`. This is the same format used in the `V4__seed_admin_user.sql` migration.
- New users always get `Role.USER`. Only an admin can promote users to `ADMIN` via the user management endpoints.
- The email uniqueness check happens at the application level. The database also enforces uniqueness via the `UNIQUE` constraint on the `email` column in the `users` table, providing a second safety net.

### Git Checkpoint

```
git add -A
git commit -m "feat: add profiles, API documentation, and user registration"
```

---

## Summary of Phases

| Phase | Focus | Key Deliverable |
|-------|-------|-----------------|
| 8 | Stabilize | Working app with login and health endpoint |
| 9 | CRUD | Complete REST API for all resources |
| 10 | DTOs | Clean API contracts with input validation |
| 11 | Errors | Consistent error responses across all endpoints |
| 12 | JWT | Stateless token-based authentication |
| 13 | Authorization | Role-based access control |
| 14 | Conflicts | Complete booking conflict detection |
| 15 | Testing | Unit and integration test coverage |
| 16 | Docker | Full-stack containerized deployment |
| 17 | Production | Profiles, docs, registration |

## Security Checklist

Before considering the project complete, verify:

- [ ] Passwords are BCrypt-hashed before storage
- [ ] JWT secret is loaded from environment variable, not hardcoded
- [ ] No password fields appear in API responses
- [ ] CSRF is disabled only because the API is stateless (no cookie-based sessions)
- [ ] Actuator endpoints are limited to health in production
- [ ] SQL injection is prevented (JPA parameterized queries)
- [ ] Input validation rejects malformed requests before reaching business logic
- [ ] `.env` files are in `.gitignore`
