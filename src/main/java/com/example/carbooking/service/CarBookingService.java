package com.example.carbooking.service;

import com.example.carbooking.dto.CarBookingResponseDto;
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
    private final CarAllocationService carAllocationService;

    private static final String SUCCESSFUL_BOOKING =
            "Car was successfully booked.";

    public CarBookingService(
            CarBookingRepository carBookingRepository,
            CarAllocationService carAllocationService) {
        this.carBookingRepository = carBookingRepository;
        this.carAllocationService = carAllocationService;
    }

    @Transactional
    public CarBookingResponseDto createCarBooking(
            CarBookingRequestDto request) {

        LocalDateTime requestedFrom = request.bookingFrom();
        LocalDateTime requestedTo =
                requestedFrom.plusDays(request.numberOfDays());

        log.info("Booking requested: type={}, from={}, days={}",
                request.carType(),
                requestedFrom,
                request.numberOfDays());

        Car availableCar = carAllocationService.allocateCar(
                request.carType(), requestedFrom, requestedTo);

        CarBooking carBooking = carBookingRepository.save(
                new CarBooking(
                        availableCar, requestedFrom, requestedTo));

        log.info("Booking created: bookingId={}, carId={}, from={}, to={}",
                carBooking.getCarBookingId(),
                availableCar.getCarId(),
                requestedFrom,
                requestedTo);

        return new CarBookingResponseDto(
                carBooking.getCarBookingId(),
                availableCar.getCarId(),
                availableCar.getCarType(),
                request.numberOfDays(),
                carBooking.getBookedFrom(),
                carBooking.getBookedTo(),
                SUCCESSFUL_BOOKING);
    }
}
