package com.example.carbooking.service;

import com.example.carbooking.exception.CarBookingUnsuccessfulException;
import com.example.carbooking.exception.NoAvailableCarFoundException;
import com.example.carbooking.model.AllocationType;
import com.example.carbooking.model.Car;
import com.example.carbooking.model.CarType;
import com.example.carbooking.repository.CarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarAllocationServiceTest {

    @InjectMocks
    private CarAllocationService carAllocationService;

    @Mock
    private CarRepository carRepository;

    @Mock
    private CarAllocationStrategyFactory strategyFactory;

    @Mock
    private CarAllocationStrategy allocationStrategy;

    private static final int CAR_ID = 1;
    private static final CarType CAR_TYPE = CarType.SEDAN;

    private LocalDateTime requestedFrom;
    private LocalDateTime requestedTo;
    private Car availableCar;

    @BeforeEach
    void setUp() {
        requestedFrom = LocalDateTime.of(2030, 1, 15, 10, 0);
        requestedTo = requestedFrom.plusDays(7);

        availableCar = new Car(CAR_TYPE);
        availableCar.setCarId(CAR_ID);

        when(strategyFactory.getStrategy(AllocationType.STANDARD))
                .thenReturn(allocationStrategy);
    }

    private void mockAvailableCarFound() {
        when(allocationStrategy.findCandidate(
                eq(CAR_TYPE),
                eq(requestedFrom),
                eq(requestedTo),
                any(LocalDateTime.class)))
                .thenReturn(Optional.of(CAR_ID));
    }

    private void mockCarLockedSuccessfully() {
        when(carRepository.findByIdForBooking(CAR_ID))
                .thenReturn(Optional.of(availableCar));
    }

    private void mockAvailabilityConfirmed() {
        when(allocationStrategy.isAvailable(
                eq(CAR_ID),
                eq(requestedFrom),
                eq(requestedTo),
                any(LocalDateTime.class)))
                .thenReturn(true);
    }

    @Test
    void shouldAllocateCarSuccessfully() {

        mockAvailableCarFound();
        mockCarLockedSuccessfully();
        mockAvailabilityConfirmed();

        Car result = carAllocationService.allocateCar(
                CAR_TYPE,
                requestedFrom,
                requestedTo,
                AllocationType.STANDARD);

        assertSame(availableCar, result);
    }

    @Test
    void testNoAvailableCarFound() {

        when(allocationStrategy.findCandidate(
                eq(CAR_TYPE),
                eq(requestedFrom),
                eq(requestedTo),
                any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        assertThrows(
                NoAvailableCarFoundException.class,
                () -> carAllocationService.allocateCar(
                        CAR_TYPE,
                        requestedFrom,
                        requestedTo,
                        AllocationType.STANDARD)
        );

        verifyNoInteractions(carRepository);
    }

    @Test
    void testCarNotFoundDuringLocking() {

        mockAvailableCarFound();

        when(carRepository.findByIdForBooking(CAR_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                CarBookingUnsuccessfulException.class,
                () -> carAllocationService.allocateCar(
                        CAR_TYPE,
                        requestedFrom,
                        requestedTo,
                        AllocationType.STANDARD)
        );

        verify(allocationStrategy, never()).isAvailable(
                anyInt(),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class));
    }

    @Test
    void testCarNoLongerAvailableAfterLocking() {

        mockAvailableCarFound();
        mockCarLockedSuccessfully();

        when(allocationStrategy.isAvailable(
                eq(CAR_ID),
                eq(requestedFrom),
                eq(requestedTo),
                any(LocalDateTime.class)))
                .thenReturn(false);

        assertThrows(
                CarBookingUnsuccessfulException.class,
                () -> carAllocationService.allocateCar(
                        CAR_TYPE,
                        requestedFrom,
                        requestedTo,
                        AllocationType.STANDARD)
        );
    }
}