package com.example.carbooking.service;

import com.example.carbooking.model.AllocationType;
import com.example.carbooking.model.CarType;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CarAllocationStrategy {

    AllocationType getAllocationType();

    Optional<Integer> findCandidate(
            CarType carType,
            LocalDateTime requestedFrom,
            LocalDateTime requestedTo,
            LocalDateTime currentTime);

    boolean isAvailable(
            Integer carId,
            LocalDateTime requestedFrom,
            LocalDateTime requestedTo,
            LocalDateTime currentTime);
}