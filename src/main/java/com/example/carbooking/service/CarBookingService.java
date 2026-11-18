package com.example.carbooking.service;

import com.example.carbooking.dto.CarBookingDto;
import com.example.carbooking.dto.CarBookingRequestDto;
import com.example.carbooking.exception.CarBookingUnsuccessfulException;
import com.example.carbooking.exception.NoAvailableCarFoundException;
import com.example.carbooking.model.Car;
import com.example.carbooking.model.CarBooking;
import com.example.carbooking.model.CarType;
import com.example.carbooking.repository.CarBookingRepository;
import com.example.carbooking.repository.CarRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class CarBookingService {

    private final CarBookingRepository carBookingRepository;
    private final CarRepository carRepository;

    public CarBookingService(CarBookingRepository carBookingRepository, CarRepository carRepository) {
        this.carBookingRepository = carBookingRepository;
        this.carRepository = carRepository;
    }

    @Transactional
    public CarBookingDto createCarBooking(CarBookingRequestDto request) {

        LocalDateTime requestedFrom = request.bookingFrom();
        LocalDateTime requestedTo = requestedFrom.plusDays(request.numberOfDays());
        CarType carType = request.carType();

        log.info("Booking requested: type={}, from={}, days={}",
                carType, requestedFrom, request.numberOfDays());

        // check if booking of any requested type of car is possible at all, without unnecessary locking, if not, don't proceed further
        Integer availableCarid = carBookingRepository.findFirstAvailableCarId(carType.name(), requestedFrom, requestedTo).orElseThrow(() -> new NoAvailableCarFoundException(carType));
        log.debug("Candidate car found: carId={}", availableCarid);

        //  acquire the lock on the car that was found to be the first available one
        Car availableCar = carRepository.findByIdForBooking(availableCarid).orElseThrow(() -> new CarBookingUnsuccessfulException(carType, availableCarid));
        log.debug("Lock acquired for carId={}", availableCarid);

        // re-check availability when lock is acquired to prevent a situation that another thread created the booking for this car between availability check and acquiring the lock
        long overlappingBookings = carBookingRepository.countOverlappingBookings(
                availableCarid, requestedFrom, requestedTo
        );

        if (overlappingBookings > 0) {
            log.warn("Booking rejected: carId={} is no longer available", availableCarid);
            throw new CarBookingUnsuccessfulException(carType, availableCarid);
        }

        CarBooking carBooking = carBookingRepository.save(new CarBooking(availableCar, requestedFrom, requestedTo));

        log.info("Booking created: bookingId={}, carId={}, from={}, to={}",
                carBooking.getCarBookingId(),
                availableCar.getCarId(),
                requestedFrom,
                requestedTo);
        return new CarBookingDto(carBooking.getCarBookingId(), availableCar.getCarId(), availableCar.getCarType(), request.numberOfDays(), carBooking.getBookedFrom(), carBooking.getBookedTo(), "SUCCESS");
    }
}
