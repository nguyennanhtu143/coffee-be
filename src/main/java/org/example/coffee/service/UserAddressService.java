package org.example.coffee.service;

import lombok.AllArgsConstructor;
import org.example.coffee.common.Common;
import org.example.coffee.dto.useraddress.UserAddressInput;
import org.example.coffee.dto.useraddress.UserAddressOutput;
import org.example.coffee.entity.UserAddressEntity;
import org.example.coffee.exceptionhandler.NotFoundException;
import org.example.coffee.repository.UserAddressRepository;
import org.example.coffee.token.TokenHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserAddressService {
    private final UserAddressRepository userAddressRepository;

    @Transactional(readOnly = true)
    public List<UserAddressOutput> getAddresses(String accessToken) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        return userAddressRepository.findAllByUserIdOrderByIsDefaultDescUpdatedAtDesc(userId)
                .stream().map(this::buildOutput).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserAddressOutput getDefaultAddress(String accessToken) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        UserAddressEntity address = userAddressRepository.findByUserIdAndIsDefault(userId, Boolean.TRUE)
                .orElseThrow(() -> new NotFoundException(Common.RECORD_NOT_FOUND));
        return buildOutput(address);
    }

    @Transactional
    public UserAddressOutput createAddress(String accessToken, UserAddressInput input) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        boolean firstAddress = !userAddressRepository.existsByUserId(userId);
        boolean isDefault = firstAddress || Boolean.TRUE.equals(input.getIsDefault());
        if (isDefault) {
            unsetDefaultAddresses(userId);
        }

        UserAddressEntity address = UserAddressEntity.builder()
                .userId(userId)
                .fullName(input.getFullName())
                .phoneNumber(input.getPhoneNumber())
                .email(input.getEmail())
                .address(input.getAddress())
                .provinceId(input.getProvinceId())
                .provinceName(input.getProvinceName())
                .toDistrictId(input.getToDistrictId())
                .districtName(input.getDistrictName())
                .toWardCode(input.getToWardCode())
                .wardName(input.getWardName())
                .isDefault(isDefault)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return buildOutput(userAddressRepository.save(address));
    }

    @Transactional
    public UserAddressOutput updateAddress(String accessToken, Long addressId, UserAddressInput input) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        UserAddressEntity address = getOwnedAddress(addressId, userId);
        if (Boolean.TRUE.equals(input.getIsDefault())) {
            unsetDefaultAddresses(userId);
            address.setIsDefault(Boolean.TRUE);
        } else if (input.getIsDefault() != null) {
            address.setIsDefault(input.getIsDefault());
        }

        address.setFullName(input.getFullName());
        address.setPhoneNumber(input.getPhoneNumber());
        address.setEmail(input.getEmail());
        address.setAddress(input.getAddress());
        address.setProvinceId(input.getProvinceId());
        address.setProvinceName(input.getProvinceName());
        address.setToDistrictId(input.getToDistrictId());
        address.setDistrictName(input.getDistrictName());
        address.setToWardCode(input.getToWardCode());
        address.setWardName(input.getWardName());
        address.setUpdatedAt(LocalDateTime.now());
        return buildOutput(userAddressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(String accessToken, Long addressId) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        UserAddressEntity address = getOwnedAddress(addressId, userId);
        userAddressRepository.delete(address);
    }

    @Transactional
    public UserAddressOutput setDefaultAddress(String accessToken, Long addressId) {
        Long userId = TokenHelper.getUserIdFromToken(accessToken);
        UserAddressEntity address = getOwnedAddress(addressId, userId);
        unsetDefaultAddresses(userId);
        address.setIsDefault(Boolean.TRUE);
        address.setUpdatedAt(LocalDateTime.now());
        return buildOutput(userAddressRepository.save(address));
    }

    private UserAddressEntity getOwnedAddress(Long addressId, Long userId) {
        return userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new NotFoundException(Common.RECORD_NOT_FOUND));
    }

    private void unsetDefaultAddresses(Long userId) {
        List<UserAddressEntity> addresses = userAddressRepository.findAllByUserIdOrderByIsDefaultDescUpdatedAtDesc(userId);
        for (UserAddressEntity address : addresses) {
            address.setIsDefault(Boolean.FALSE);
            address.setUpdatedAt(LocalDateTime.now());
        }
        userAddressRepository.saveAll(addresses);
    }

    private UserAddressOutput buildOutput(UserAddressEntity address) {
        return UserAddressOutput.builder()
                .addressId(address.getId())
                .fullName(address.getFullName())
                .phoneNumber(address.getPhoneNumber())
                .email(address.getEmail())
                .address(address.getAddress())
                .provinceId(address.getProvinceId())
                .provinceName(address.getProvinceName())
                .toDistrictId(address.getToDistrictId())
                .districtName(address.getDistrictName())
                .toWardCode(address.getToWardCode())
                .wardName(address.getWardName())
                .isDefault(address.getIsDefault())
                .build();
    }
}
