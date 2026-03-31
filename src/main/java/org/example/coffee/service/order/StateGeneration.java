package org.example.coffee.service.order;

import lombok.AllArgsConstructor;
import org.example.coffee.common.Common;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class StateGeneration {
    private final BaseState baseState;
    private final CancellationState cancellationState;

    public StateOrder findSateBy(String state) {
        if (state.equals(Common.CANCELED)) {
            return cancellationState;
        }
        return baseState;
    }
}
