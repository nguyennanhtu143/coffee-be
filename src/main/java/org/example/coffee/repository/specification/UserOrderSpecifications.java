package org.example.coffee.repository.specification;

import org.example.coffee.entity.UserOrderEntity;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class UserOrderSpecifications {
    private UserOrderSpecifications() {
    }

    public static Specification<UserOrderEntity> adminOrders(String state, Long orderId, String phoneNumber,
                                                             LocalDateTime createdFrom, LocalDateTime createdTo) {
        return Specification
                .where(hasState(state))
                .and(hasOrderId(orderId))
                .and(phoneNumberContains(phoneNumber))
                .and(createdAtFrom(createdFrom))
                .and(createdAtTo(createdTo));
    }

    private static Specification<UserOrderEntity> hasState(String state) {
        return (root, query, criteriaBuilder) -> {
            if (state == null || state.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("state"), state.trim());
        };
    }

    private static Specification<UserOrderEntity> hasOrderId(Long orderId) {
        return (root, query, criteriaBuilder) -> {
            if (orderId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("id"), orderId);
        };
    }

    private static Specification<UserOrderEntity> phoneNumberContains(String phoneNumber) {
        return (root, query, criteriaBuilder) -> {
            if (phoneNumber == null || phoneNumber.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(root.get("phoneNumber"), "%" + phoneNumber.trim() + "%");
        };
    }

    private static Specification<UserOrderEntity> createdAtFrom(LocalDateTime createdFrom) {
        return (root, query, criteriaBuilder) -> {
            if (createdFrom == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom);
        };
    }

    private static Specification<UserOrderEntity> createdAtTo(LocalDateTime createdTo) {
        return (root, query, criteriaBuilder) -> {
            if (createdTo == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdTo);
        };
    }
}
