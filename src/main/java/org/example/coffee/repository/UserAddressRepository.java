package org.example.coffee.repository;

import org.example.coffee.entity.UserAddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddressEntity, Long> {
    List<UserAddressEntity> findAllByUserIdOrderByIsDefaultDescUpdatedAtDesc(Long userId);

    Optional<UserAddressEntity> findByUserIdAndIsDefault(Long userId, Boolean isDefault);

    Optional<UserAddressEntity> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserId(Long userId);
}
