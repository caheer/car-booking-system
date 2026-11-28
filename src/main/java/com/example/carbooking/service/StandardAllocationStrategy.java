package com.example.carbooking.service;

import com.example.carbooking.model.CarType;
import com.example.carbooking.repository.CarBookingRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class StandardAllocationStrategy
        implements CarAllocationStrategy {

    private final CarBookingRepository carBookingRepository;

    public StandardAllocationStrategy(
            CarBookingRepository carBookingRepository) {
        this.carBookingRepository = carBookingRepository;
    }

    @Override
    public Optional<Integer> findCandidate(
            CarType carType,
            LocalDateTime requestedFrom,
            LocalDateTime requestedTo, LocalDateTime currentTime) {

        return carBookingRepository.findFirstAvailableCarId(
                carType.name(),
                requestedFrom,
                requestedTo);
    }

    @Override
    public boolean isAvailable(
            Integer carId,
            LocalDateTime requestedFrom,
            LocalDateTime requestedTo, LocalDateTime currentTime) {

        return carBookingRepository.countOverlappingBookings(
                carId,
                requestedFrom,
                requestedTo) == 0;
    }
}