package com.example.carbooking.service;

import com.example.carbooking.dto.CarBookingResponseDto;
import com.example.carbooking.dto.CarBookingRequestDto;
import com.example.carbooking.exception.CarBookingUnsuccessfulException;
import com.example.carbooking.exception.NoAvailableCarFoundException;
import com.example.carbooking.model.AllocationType;
import com.example.carbooking.model.Car;
import com.example.carbooking.model.CarBooking;
import com.example.carbooking.model.CarType;
import com.example.carbooking.repository.CarBookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarBookingServiceTest {

    @InjectMocks
    private CarBookingService carBookingService;

    @Mock
    private CarBookingRepository carBookingRepository;

    @Mock
    private CarAllocator carAllocator;

    private static final int CAR_ID = 1;
    private static final int NUMBER_OF_DAYS = 7;
    private static final CarType CAR_TYPE = CarType.SEDAN;

    private LocalDateTime requestedFrom;
    private LocalDateTime requestedTo;
    private Car availableCar;
    private CarBookingRequestDto request;

    @BeforeEach
    void setUp() {
        requestedFrom = LocalDateTime.of(2030, 1, 15, 10, 0);
        requestedTo = requestedFrom.plusDays(NUMBER_OF_DAYS);

        availableCar = new Car(CAR_TYPE);
        availableCar.setCarId(CAR_ID);

        request = new CarBookingRequestDto(
                CAR_TYPE,
                requestedFrom,
                NUMBER_OF_DAYS,
                AllocationType.STANDARD);
    }


    private void mockSuccessfulCarAllocation() {
        when(carAllocator.allocateCar(
                CAR_TYPE,
                requestedFrom,
                requestedTo,
                AllocationType.STANDARD))
                .thenReturn(availableCar);
    }

    @Test
    void createSuccessfulCarBooking() {

        when(carAllocator.allocateCar(
                CAR_TYPE,
                requestedFrom,
                requestedTo,
                AllocationType.STANDARD))
                .thenReturn(availableCar);

        CarBooking savedBooking =
                new CarBooking(availableCar, requestedFrom, requestedTo);

        savedBooking.setCarBookingId(1);

        when(carBookingRepository.save(any(CarBooking.class)))
                .thenReturn(savedBooking);

        CarBookingResponseDto result =
                carBookingService.createCarBooking(request);

        assertNotNull(result);
        assertEquals(CAR_ID, result.carId());

        ArgumentCaptor<CarBooking> captor =
                ArgumentCaptor.forClass(CarBooking.class);

        verify(carBookingRepository).save(captor.capture());

        CarBooking booking = captor.getValue();

        assertEquals(CAR_ID, booking.getCar().getCarId());
        assertEquals(requestedFrom, booking.getBookedFrom());
        assertEquals(requestedTo, booking.getBookedTo());
    }

    @Test
    void testNoAvailableCarFound() {

        when(carAllocator.allocateCar(
                CAR_TYPE,
                requestedFrom,
                requestedTo,
                AllocationType.STANDARD))
                .thenThrow(new NoAvailableCarFoundException(CAR_TYPE));

        assertThrows(
                NoAvailableCarFoundException.class,
                () -> carBookingService.createCarBooking(request)
        );

        verify(carBookingRepository, never())
                .save(any(CarBooking.class));
    }

    @Test
    void testCarAllocationFails() {

        when(carAllocator.allocateCar(
                CAR_TYPE,
                requestedFrom,
                requestedTo,
                AllocationType.STANDARD))
                .thenThrow(new CarBookingUnsuccessfulException(
                        CAR_TYPE, CAR_ID));

        assertThrows(
                CarBookingUnsuccessfulException.class,
                () -> carBookingService.createCarBooking(request)
        );

        verify(carBookingRepository, never())
                .save(any(CarBooking.class));
    }


    @Test
    void testSaveBookingFails() {

        mockSuccessfulCarAllocation();

        when(carBookingRepository.save(any(CarBooking.class)))
                .thenThrow(
                        new DataIntegrityViolationException("Save failed")
                );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> carBookingService.createCarBooking(request)
        );

        verify(carBookingRepository)
                .save(any(CarBooking.class));
    }

    @ParameterizedTest
    @EnumSource(CarType.class)
    void shouldBookCarOfRequestedType(CarType carType) {

        Car car = new Car(carType);
        car.setCarId(CAR_ID);

        CarBookingRequestDto request = new CarBookingRequestDto(
                carType,
                requestedFrom,
                NUMBER_OF_DAYS,
                AllocationType.STANDARD
        );

        when(carAllocator.allocateCar(
                carType,
                requestedFrom,
                requestedTo,
                AllocationType.STANDARD))
                .thenReturn(car);

        CarBooking savedBooking =
                new CarBooking(car, requestedFrom, requestedTo);

        savedBooking.setCarBookingId(1);

        when(carBookingRepository.save(any(CarBooking.class)))
                .thenReturn(savedBooking);

        CarBookingResponseDto result =
                carBookingService.createCarBooking(request);

        assertEquals(carType, result.carType());
        assertEquals(CAR_ID, result.carId());
    }
}