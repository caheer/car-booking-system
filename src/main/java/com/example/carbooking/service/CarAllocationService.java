package com.example.carbooking.service;

import com.example.carbooking.exception.CarBookingUnsuccessfulException;
import com.example.carbooking.exception.NoAvailableCarFoundException;
import com.example.carbooking.model.Car;
import com.example.carbooking.model.CarType;
import com.example.carbooking.repository.CarRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class CarAllocationService implements CarAllocator {

    private final CarRepository carRepository;
    private final CarAllocationStrategy allocationStrategy;

    public CarAllocationService(
            CarRepository carRepository,
            @Qualifier("standardAllocationStrategy")
            CarAllocationStrategy allocationStrategy) {

        this.carRepository = carRepository;
        this.allocationStrategy = allocationStrategy;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Car allocateCar(
            CarType carType,
            LocalDateTime requestedFrom,
            LocalDateTime requestedTo) {

        LocalDateTime currentTime = LocalDateTime.now();

        Integer availableCarId = allocationStrategy
                .findCandidate(carType, requestedFrom, requestedTo, currentTime)
                .orElseThrow(() ->
                        new NoAvailableCarFoundException(carType));

        log.debug("Candidate car found: carId={}", availableCarId);

        Car availableCar = carRepository
                .findByIdForBooking(availableCarId)
                .orElseThrow(() ->
                        new CarBookingUnsuccessfulException(
                                carType, availableCarId));

        log.debug("Lock acquired for carId={}", availableCarId);

        if (!allocationStrategy.isAvailable(
                availableCarId, requestedFrom, requestedTo, currentTime)) {

            log.warn("Car {} is no longer available", availableCarId);

            throw new CarBookingUnsuccessfulException(
                    carType, availableCarId);
        }

        return availableCar;
    }
}