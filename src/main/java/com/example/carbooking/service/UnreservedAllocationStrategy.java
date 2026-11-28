package com.example.carbooking.service;

import com.example.carbooking.model.CarType;
import com.example.carbooking.repository.CarBookingRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class UnreservedAllocationStrategy
        implements CarAllocationStrategy {

    private final CarBookingRepository carBookingRepository;

    public UnreservedAllocationStrategy(
            CarBookingRepository carBookingRepository) {
        this.carBookingRepository = carBookingRepository;
    }

    @Override
    public Optional<Integer> findCandidate(
            CarType carType,
            LocalDateTime requestedFrom,
            LocalDateTime requestedTo,
            LocalDateTime currentTime) {

        return carBookingRepository.findFirstUnreservedCarId(
                carType.name(), currentTime);
    }

    @Override
    public boolean isAvailable(
            Integer carId,
            LocalDateTime requestedFrom,
            LocalDateTime requestedTo,
            LocalDateTime currentTime) {

        return carBookingRepository.countOngoingAndFutureBookings(
                carId, currentTime) == 0;
    }
}