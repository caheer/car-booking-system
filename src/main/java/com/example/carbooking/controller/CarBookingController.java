package com.example.carbooking.controller;

import com.example.carbooking.dto.CarBookingDto;
import com.example.carbooking.dto.CarBookingRequestDto;
import com.example.carbooking.service.CarBookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/carBooking")
public class CarBookingController {

    private final CarBookingService carBookingService;

    public CarBookingController(CarBookingService carBookingService) {
        this.carBookingService = carBookingService;
    }

    @PostMapping
    public ResponseEntity<CarBookingDto> createCarBooking(
            @Valid @RequestBody CarBookingRequestDto request) {

        CarBookingDto result = carBookingService.createCarBooking(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}