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
import java.util.Optional;

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
    private CarRepository carRepository;

    private static final int CAR_ID = 1;
    private static final int NUMBER_OF_DAYS = 7;
    private static final CarType CAR_TYPE = CarType.SEDAN;

    private LocalDateTime requestedFrom;
    private LocalDateTime requestedTo;
    private Car availableCar;
    private CarBookingRequestDto request;

    @BeforeEach
    void setUp() {
        requestedFrom = LocalDateTime.of(2027, 1, 15, 10, 0);
        requestedTo = requestedFrom.plusDays(NUMBER_OF_DAYS);

        availableCar = new Car(CAR_TYPE);
        availableCar.setCarId(CAR_ID);

        request = new CarBookingRequestDto(
                CAR_TYPE, requestedFrom, NUMBER_OF_DAYS);
    }

    private void mockAvailableCarFound() {
        when(carBookingRepository.findFirstAvailableCarId(
                CAR_TYPE.name(), requestedFrom, requestedTo))
                .thenReturn(Optional.of(CAR_ID));
    }

    private void mockCarLockedSuccessfully() {
        when(carRepository.findByIdForBooking(CAR_ID))
                .thenReturn(Optional.of(availableCar));
    }

    private void mockNoOverlappingBookings() {
        when(carBookingRepository.countOverlappingBookings(
                CAR_ID, requestedFrom, requestedTo))
                .thenReturn(0L);
    }

    @Test
    void createSuccessfulCarBooking() {
        mockAvailableCarFound();
        mockCarLockedSuccessfully();
        mockNoOverlappingBookings();

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
        when(carBookingRepository.findFirstAvailableCarId(
                CAR_TYPE.name(), requestedFrom, requestedTo))
                .thenReturn(Optional.empty());

        assertThrows(
                NoAvailableCarFoundException.class,
                () -> carBookingService.createCarBooking(request)
        );

        verify(carBookingRepository, never())
                .save(any(CarBooking.class));
    }

    @Test
    void testCarNotFoundDuringLocking() {
        mockAvailableCarFound();

        when(carRepository.findByIdForBooking(CAR_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                CarBookingUnsuccessfulException.class,
                () -> carBookingService.createCarBooking(request)
        );

        verify(carBookingRepository, never())
                .save(any(CarBooking.class));
    }

    @Test
    void testOverlappingBookings() {
        mockAvailableCarFound();
        mockCarLockedSuccessfully();

        when(carBookingRepository.countOverlappingBookings(
                CAR_ID, requestedFrom, requestedTo))
                .thenReturn(1L);

        assertThrows(
                CarBookingUnsuccessfulException.class,
                () -> carBookingService.createCarBooking(request)
        );

        verify(carBookingRepository, never())
                .save(any(CarBooking.class));
    }

    @Test
    void testSaveBookingFails() {
        mockAvailableCarFound();
        mockCarLockedSuccessfully();
        mockNoOverlappingBookings();

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
                carType, requestedFrom, NUMBER_OF_DAYS
        );

        when(carBookingRepository.findFirstAvailableCarId(
                carType.name(), requestedFrom, requestedTo))
                .thenReturn(Optional.of(CAR_ID));

        when(carRepository.findByIdForBooking(CAR_ID))
                .thenReturn(Optional.of(car));

        when(carBookingRepository.countOverlappingBookings(
                CAR_ID, requestedFrom, requestedTo))
                .thenReturn(0L);

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