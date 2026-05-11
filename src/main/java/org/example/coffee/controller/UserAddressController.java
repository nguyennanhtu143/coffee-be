package org.example.coffee.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.example.coffee.dto.useraddress.UserAddressInput;
import org.example.coffee.dto.useraddress.UserAddressOutput;
import org.example.coffee.service.UserAddressService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/user-address")
public class UserAddressController {
    private final UserAddressService userAddressService;

    @GetMapping("/get-addresses")
    public List<UserAddressOutput> getAddresses(@RequestHeader("Authorization") String accessToken) {
        return userAddressService.getAddresses(accessToken);
    }

    @GetMapping("/get-default")
    public UserAddressOutput getDefaultAddress(@RequestHeader("Authorization") String accessToken) {
        return userAddressService.getDefaultAddress(accessToken);
    }

    @PostMapping("/create")
    public UserAddressOutput createAddress(@RequestHeader("Authorization") String accessToken,
                                           @Valid @RequestBody UserAddressInput input) {
        return userAddressService.createAddress(accessToken, input);
    }

    @PostMapping("/update")
    public UserAddressOutput updateAddress(@RequestHeader("Authorization") String accessToken,
                                           @RequestParam Long addressId,
                                           @Valid @RequestBody UserAddressInput input) {
        return userAddressService.updateAddress(accessToken, addressId, input);
    }

    @DeleteMapping("/delete")
    public void deleteAddress(@RequestHeader("Authorization") String accessToken,
                              @RequestParam Long addressId) {
        userAddressService.deleteAddress(accessToken, addressId);
    }

    @PostMapping("/set-default")
    public UserAddressOutput setDefaultAddress(@RequestHeader("Authorization") String accessToken,
                                               @RequestParam Long addressId) {
        return userAddressService.setDefaultAddress(accessToken, addressId);
    }
}
