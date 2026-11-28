package com.example.carbooking.service;

import com.example.carbooking.model.AllocationType;
import com.example.carbooking.model.Car;
import com.example.carbooking.model.CarType;

import java.time.LocalDateTime;

public interface CarAllocator {

    Car allocateCar(
            CarType carType,
            LocalDateTime requestedFrom,
            LocalDateTime requestedTo,
            AllocationType allocationType);
}