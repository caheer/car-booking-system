package com.example.carbooking.service;

import com.example.carbooking.model.AllocationType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CarAllocationStrategyFactory {

    private final Map<AllocationType, CarAllocationStrategy> strategies;

    public CarAllocationStrategyFactory(
            List<CarAllocationStrategy> strategies) {

        this.strategies = strategies.stream()
                .collect(Collectors.toMap(
                        CarAllocationStrategy::getAllocationType,
                        Function.identity()));
    }

    public CarAllocationStrategy getStrategy(AllocationType allocationType) {

        CarAllocationStrategy strategy = strategies.get(allocationType);

        if (strategy == null) {
            throw new IllegalArgumentException(
                    "Unsupported allocation type: " + allocationType);
        }

        return strategy;
    }
}