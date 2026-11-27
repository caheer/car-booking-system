package com.example.carbooking.service;

import com.example.carbooking.exception.CarBookingUnsuccessfulException;
import com.example.carbooking.exception.NoAvailableCarFoundException;
import com.example.carbooking.model.Car;
import com.example.carbooking.model.CarType;
import com.example.carbooking.repository.CarBookingRepository;
import com.example.carbooking.repository.CarRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class CarAllocationService {

    private final CarRepository carRepository;
    private final CarBookingRepository carBookingRepository;

    public CarAllocationService(
            CarRepository carRepository,
            CarBookingRepository carBookingRepository) {
        this.carRepository = carRepository;
        this.carBookingRepository = carBookingRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Car allocateCar(
            CarType carType,
            LocalDateTime requestedFrom,
            LocalDateTime requestedTo) {

        Integer availableCarId = carBookingRepository
                .findFirstAvailableCarId(
                        carType.name(), requestedFrom, requestedTo)
                .orElseThrow(() ->
                        new NoAvailableCarFoundException(carType));

        log.debug("Candidate car found: carId={}", availableCarId);

        Car availableCar = carRepository
                .findByIdForBooking(availableCarId)
                .orElseThrow(() ->
                        new CarBookingUnsuccessfulException(
                                carType, availableCarId));

        log.debug("Lock acquired for carId={}", availableCarId);

        long overlappingBookings = carBookingRepository
                .countOverlappingBookings(
                        availableCarId, requestedFrom, requestedTo);

        if (overlappingBookings > 0) {
            log.warn("Booking rejected: carId={} is no longer available",
                    availableCarId);

            throw new CarBookingUnsuccessfulException(
                    carType, availableCarId);
        }

        return availableCar;
    }
}