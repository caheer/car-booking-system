
package com.example.carbooking.controller;

import com.example.carbooking.dto.CarBookingResponseDto;
import com.example.carbooking.exception.NoAvailableCarFoundException;
import com.example.carbooking.model.CarType;
import com.example.carbooking.service.CarBookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CarBookingController.class)
class CarBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CarBookingService carBookingService;

    @Test
    void shouldCreateBooking() throws Exception {

        LocalDateTime from = LocalDateTime.of(2030, 1, 15, 10, 0);

        CarBookingResponseDto response = new CarBookingResponseDto(
                1,
                5,
                CarType.SEDAN,
                3,
                from,
                from.plusDays(3),
                "Car was successfully booked."
        );

        when(carBookingService.createCarBooking(any()))
                .thenReturn(response);

        mockMvc.perform(post("/carBooking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "carType": "SEDAN",
                                  "bookingFrom": "2030-01-15T10:00:00",
                                  "numberOfDays": 3,
                                  "allocationType": "STANDARD"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.carBookingId").value(1))
                .andExpect(jsonPath("$.carId").value(5))
                .andExpect(jsonPath("$.carType").value("SEDAN"))
                .andExpect(jsonPath("$.numberOfDays").value(3))
                .andExpect(jsonPath("$.bookingMessage")
                        .value("Car was successfully booked."));

        verify(carBookingService).createCarBooking(any());
    }

    @Test
    void shouldRejectInvalidNumberOfDays() throws Exception {

        mockMvc.perform(post("/carBooking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "carType": "SEDAN",
                              "bookingFrom": "2030-01-15T10:00:00",
                              "numberOfDays": 0,
                              "allocationType": "STANDARD"
                            }
                            """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(carBookingService);
    }

    @Test
    void shouldReturnConflictWhenNoCarAvailable() throws Exception {

        when(carBookingService.createCarBooking(any()))
                .thenThrow(new NoAvailableCarFoundException(CarType.SEDAN));

        mockMvc.perform(post("/carBooking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "carType": "SEDAN",
                                  "bookingFrom": "2030-01-15T10:00:00",
                                  "numberOfDays": 3,
                                  "allocationType": "STANDARD"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().string(
                        "No car of type: SEDAN is available for the requested booking period."
                ));

        verify(carBookingService).createCarBooking(any());
    }
}
