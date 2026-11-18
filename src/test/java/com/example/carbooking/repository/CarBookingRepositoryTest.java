package com.example.carbooking.repository;

import com.example.carbooking.model.Car;
import com.example.carbooking.model.CarBooking;
import com.example.carbooking.model.CarType;
import com.example.carbooking.service.CarBookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@DataJpaTest
class CarBookingRepositoryTest {

    @Autowired
    private CarBookingRepository carBookingRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private TestEntityManager entityManager;


    @Test
    void findFirstAvailableCarId() {

        Car sedan1 = carRepository.save(new Car(CarType.SEDAN));
        Car sedan2 = carRepository.save(new Car(CarType.SEDAN));

        LocalDateTime bookedFrom =
                LocalDateTime.of(2027, 1, 10, 10, 0);
        LocalDateTime bookedTo =
                LocalDateTime.of(2027, 1, 17, 10, 0);

        // Create booking for sedan 1
        CarBooking existingBooking =
                new CarBooking(sedan1, bookedFrom, bookedTo);

        carBookingRepository.save(existingBooking);

        entityManager.flush();
        entityManager.clear();

        // Request another Sedan during the same period
        LocalDateTime requestedFrom =
                LocalDateTime.of(2027, 1, 12, 10, 0);
        LocalDateTime requestedTo =
                LocalDateTime.of(2027, 1, 15, 10, 0);

        Optional<Integer> availableCarId =
                carBookingRepository.findFirstAvailableCarId(
                        CarType.SEDAN.name(),
                        requestedFrom,
                        requestedTo
                );


        assertTrue(availableCarId.isPresent());
        assertEquals(sedan2.getCarId(), availableCarId.get());
    }

    @Test
    void countOverlappingBookings() {

        Car car = new Car(CarType.SEDAN);
        Car savedCar = carRepository.save(car);

        LocalDateTime bookedFrom =
                LocalDateTime.of(2027, 1, 10, 10, 0);

        LocalDateTime bookedTo =
                LocalDateTime.of(2027, 1, 17, 10, 0);

        CarBooking booking =
                new CarBooking(savedCar, bookedFrom, bookedTo);

        carBookingRepository.save(booking);

        entityManager.flush();
        entityManager.clear();

        Integer carId = savedCar.getCarId();

        // Requested period within the existing booking
        assertEquals(1L, carBookingRepository.countOverlappingBookings(
                carId,
                LocalDateTime.of(2027, 1, 12, 10, 0),
                LocalDateTime.of(2027, 1, 15, 10, 0)
        ));

        // Booking period overlap at the beginning
        assertEquals(1L, carBookingRepository.countOverlappingBookings(
                carId,
                LocalDateTime.of(2027, 1, 5, 10, 0),
                LocalDateTime.of(2027, 1, 12, 10, 0)
        ));

        // Booking period overlap at the end
        assertEquals(1L, carBookingRepository.countOverlappingBookings(
                carId,
                LocalDateTime.of(2027, 1, 15, 10, 0),
                LocalDateTime.of(2027, 1, 20, 10, 0)
        ));

        // New booking begins exactly when the previous one ends
        assertEquals(0L, carBookingRepository.countOverlappingBookings(
                carId,
                LocalDateTime.of(2027, 1, 17, 10, 0),
                LocalDateTime.of(2027, 1, 20, 10, 0)
        ));

        // New booking ends exactly when existing booking starts
        assertEquals(0L, carBookingRepository.countOverlappingBookings(
                carId,
                LocalDateTime.of(2027, 1, 5, 10, 0),
                LocalDateTime.of(2027, 1, 10, 10, 0)
        ));

        assertEquals(1L, carBookingRepository.countOverlappingBookings(
                carId,
                LocalDateTime.of(2027, 1, 5, 10, 0),
                LocalDateTime.of(2027, 1, 20, 10, 0)
        ));
    }
}