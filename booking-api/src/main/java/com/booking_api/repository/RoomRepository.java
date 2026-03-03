package com.booking_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.booking_api.model.Room;

@Repository 
public interface RoomRepository extends JpaRepository<Room, Long>
{
    
}
