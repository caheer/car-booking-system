
package com.example.carbooking.service;

import com.example.carbooking.dto.CarBookingRequestDto;
import com.example.carbooking.exception.CarBookingUnsuccessfulException;
import com.example.carbooking.exception.NoAvailableCarFoundException;
import com.example.carbooking.model.Car;
import com.example.carbooking.model.CarType;
import com.example.carbooking.repository.CarBookingRepository;
import com.example.carbooking.repository.CarRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/car_booking_test_db",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("postgres")
class CarBookingConcurrencyTest {

    @Autowired
    private CarBookingService carBookingService;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private CarBookingRepository carBookingRepository;

    @Test
    void onlyOneConcurrentRequestCanBookTheSameCar() throws Exception {

        // The test database should contain no existing data.
        assertEquals(0, carRepository.count());
        assertEquals(0, carBookingRepository.count());

        Car car = carRepository.save(new Car(CarType.SEDAN));

        LocalDateTime from = LocalDateTime.of(2030, 1, 15, 10, 0);

        CarBookingRequestDto request =
                new CarBookingRequestDto(CarType.SEDAN, from, 7);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Boolean> bookingTask = () -> {

            ready.countDown();

            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Start timeout");
            }

            try {
                carBookingService.createCarBooking(request);
                return true;

            } catch (NoAvailableCarFoundException |
                     CarBookingUnsuccessfulException ex) {

                return false;
            }
        };

        try (var executor = Executors.newFixedThreadPool(2)) {

            var first = executor.submit(bookingTask);
            var second = executor.submit(bookingTask);

            // Both threads are ready to issue their requests.
            assertTrue(ready.await(10, TimeUnit.SECONDS));

            // Start both requests.
            start.countDown();

            List<Boolean> results = List.of(
                    first.get(15, TimeUnit.SECONDS),
                    second.get(15, TimeUnit.SECONDS)
            );

            // Exactly one request must succeed.
            assertEquals(1, results.stream()
                    .filter(Boolean::booleanValue)
                    .count());
        }

        // Exactly one booking must exist in the database.
        assertEquals(1, carBookingRepository.count());

        var savedBooking = carBookingRepository.findAll().getFirst();

        assertEquals(car.getCarId(), savedBooking.getCar().getCarId());
        assertEquals(from, savedBooking.getBookedFrom());
        assertEquals(from.plusDays(7), savedBooking.getBookedTo());
    }
}