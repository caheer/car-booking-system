package com.example.carbooking.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "car_booking")
public class CarBooking {

    public CarBooking(Car car, LocalDateTime bookedFrom, LocalDateTime bookedTo) {
        this.car = car;
        this.bookedFrom = bookedFrom;
        this.bookedTo = bookedTo;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "car_booking_id")
    private Integer carBookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @Column(name = "booked_from")
    private LocalDateTime bookedFrom;

    @Column(name = "booked_to")
    private LocalDateTime bookedTo;
}
