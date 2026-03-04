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
