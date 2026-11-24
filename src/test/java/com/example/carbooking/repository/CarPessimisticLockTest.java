
package com.example.carbooking.repository;

import com.example.carbooking.model.Car;
import com.example.carbooking.model.CarType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/car_booking_test_db",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("postgres")
class CarPessimisticLockTest {

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void secondTransactionShouldWaitUntilFirstReleasesCarLock()
            throws Exception {

        Car car = carRepository.save(new Car(CarType.SEDAN));
        Integer carId = car.getCarId();

        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondAttempting = new CountDownLatch(1);

        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {

            Future<?> first = executor.submit(() -> {
                tx.executeWithoutResult(status -> {
                    carRepository.findByIdForBooking(carId)
                            .orElseThrow();

                    firstLocked.countDown();

                    try {
                        if (!releaseFirst.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Timeout");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                });
            });

            try {
                assertTrue(firstLocked.await(5, TimeUnit.SECONDS));

                Future<?> second = executor.submit(() -> {
                    tx.executeWithoutResult(status -> {
                        secondAttempting.countDown();

                        carRepository.findByIdForBooking(carId)
                                .orElseThrow();
                    });
                });

                assertTrue(secondAttempting.await(5, TimeUnit.SECONDS));

                // The second transaction should still be blocked.
                assertThrows(
                        TimeoutException.class,
                        () -> second.get(500, TimeUnit.MILLISECONDS)
                );

                // Allow the first transaction to commit and release its lock.
                releaseFirst.countDown();

                first.get(5, TimeUnit.SECONDS);
                second.get(5, TimeUnit.SECONDS);

                // Both transactions have now completed.
            } finally {
                releaseFirst.countDown();
            }

        } finally {
            // Delete only the car created by this test.
            carRepository.deleteById(carId);
        }
    }
}
