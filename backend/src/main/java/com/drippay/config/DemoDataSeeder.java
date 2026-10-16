package com.drippay.config;

import com.drippay.domain.entity.User;
import com.drippay.domain.entity.Wallet;
import com.drippay.domain.enums.UserRole;
import com.drippay.repository.UserRepository;
import com.drippay.repository.WalletRepository;
import com.drippay.util.WalletIdGenerator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
@ConditionalOnProperty(name = "drippay.seed-demo", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder {

    @Bean
    CommandLineRunner seedDemoUsers(UserRepository userRepository,
                                    WalletRepository walletRepository,
                                    PasswordEncoder passwordEncoder) {
        return args -> {
            ensureDemoUser(userRepository, walletRepository, passwordEncoder,
                    "demo@drippay.app", "Demo User", UserRole.USER, "Demo1234!");
            ensureDemoUser(userRepository, walletRepository, passwordEncoder,
                    "admin@drippay.app", "DripPay Admin", UserRole.ADMIN, "Admin1234!");
        };
    }

    private static void ensureDemoUser(UserRepository userRepository,
                                       WalletRepository walletRepository,
                                       PasswordEncoder passwordEncoder,
                                       String email,
                                       String fullName,
                                       UserRole role,
                                       String rawPassword) {
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setFullName(fullName);
            u.setPasswordHash(passwordEncoder.encode(rawPassword));
            u.setRole(role);
            return userRepository.save(u);
        });

        if (walletRepository.findByUserId(user.getId()).isEmpty()) {
            Wallet w = new Wallet();
            w.setUserId(user.getId());
            w.setPublicWalletId(WalletIdGenerator.generate());
            w.setBalance(BigDecimal.ZERO);
            walletRepository.save(w);
        }
    }
}
