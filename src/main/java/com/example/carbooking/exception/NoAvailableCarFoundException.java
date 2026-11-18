package com.example.carbooking.exception;

import com.example.carbooking.model.CarType;

public class NoAvailableCarFoundException extends RuntimeException {
    public NoAvailableCarFoundException(CarType carType) {
        super("No car of type: " + carType.name() + " is available for the requested booking period.");
    }
}