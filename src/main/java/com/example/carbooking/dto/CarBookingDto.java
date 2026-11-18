package com.example.carbooking.dto;

import com.example.carbooking.model.CarType;

import java.time.LocalDateTime;

public record CarBookingDto(
        int carBookingId,
        int carId,
        CarType carType,
        int numberOfDays,
        LocalDateTime bookedFrom,
        LocalDateTime bookedTo,
        String bookingMessage
) {
}
