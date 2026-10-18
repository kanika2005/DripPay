package com.drippay.service;

import com.drippay.domain.entity.User;
import com.drippay.exception.ApiException;
import com.drippay.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferPinService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TransferPinService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void setPin(Long userId, String pin) {
        if (pin == null || !pin.matches("\\d{4,6}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PIN must be 4–6 digits");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        user.setTransferPinHash(passwordEncoder.encode(pin));
        userRepository.save(user);
    }

    public void verifyPin(User user, String pin) {
        if (user.getTransferPinHash() == null || user.getTransferPinHash().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Set a transfer PIN under Profile first");
        }
        if (pin == null || !passwordEncoder.matches(pin, user.getTransferPinHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid transfer PIN");
        }
    }

    public boolean hasPin(Long userId) {
        return userRepository.findById(userId)
                .map(u -> u.getTransferPinHash() != null && !u.getTransferPinHash().isBlank())
                .orElse(false);
    }
}
