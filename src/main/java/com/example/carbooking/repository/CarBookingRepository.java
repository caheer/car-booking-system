package com.example.carbooking.repository;

import com.example.carbooking.model.CarBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CarBookingRepository extends JpaRepository<CarBooking, Integer> {


    @Query(value = """
            SELECT c.car_id
            FROM car c
            WHERE c.car_type = :carType
              AND NOT EXISTS (
                  SELECT 1
                  FROM car_booking b
                  WHERE b.car_id = c.car_id
                    AND b.booked_from < :requestedTo
                    AND b.booked_to > :requestedFrom
              )
            ORDER BY c.car_id
            LIMIT 1
            """, nativeQuery = true)
    Optional<Integer> findFirstAvailableCarId(
            @Param("carType") String carType,
            @Param("requestedFrom") LocalDateTime requestedFrom,
            @Param("requestedTo") LocalDateTime requestedTo
    );

    @Query("""
            SELECT COUNT(b) FROM CarBooking b
            WHERE b.car.carId = :carId
              AND b.bookedFrom < :requestedTo
              AND b.bookedTo > :requestedFrom
            """)
    long countOverlappingBookings(
            Integer carId,
            LocalDateTime requestedFrom,
            LocalDateTime requestedTo
    );

    @Query(value = """
        SELECT c.car_id
        FROM car c
        WHERE c.car_type = :carType
          AND NOT EXISTS (
              SELECT 1
              FROM car_booking b
              WHERE b.car_id = c.car_id
                AND b.booked_to > :currentTime
          )
        ORDER BY c.car_id
        LIMIT 1
        """, nativeQuery = true)
    Optional<Integer> findFirstUnreservedCarId(
            @Param("carType") String carType,
            @Param("currentTime") LocalDateTime currentTime);

    @Query("""
        SELECT COUNT(b)
        FROM CarBooking b
        WHERE b.car.carId = :carId
          AND b.bookedTo > :currentTime
        """)
    long countOngoingAndFutureBookings(
            @Param("carId") Integer carId,
            @Param("currentTime") LocalDateTime currentTime);
}
