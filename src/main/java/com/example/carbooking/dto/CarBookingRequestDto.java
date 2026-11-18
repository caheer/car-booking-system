package com.example.carbooking.dto;

import com.example.carbooking.model.CarType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CarBookingRequestDto(

        @NotNull
        CarType carType,

        @NotNull
        @Future
        LocalDateTime bookingFrom,

        @Min(1)
        int numberOfDays
) {
}