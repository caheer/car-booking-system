package com.example.carbooking.exception;

import com.example.carbooking.model.CarType;

public class CarBookingUnsuccessfulException extends RuntimeException {
    public CarBookingUnsuccessfulException(CarType carType, Integer carId) {
        super("Attempt to book car no: " + carId + ", of type " + carType.name() + "failed,please try again");
    }
}