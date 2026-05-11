package org.example.coffee.repository;

import org.example.coffee.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    UserEntity findByUsername(String username);

    UserEntity findByEmail(String email);

    List<UserEntity> findAllByIdIn(Set<Long> userIds);
}
