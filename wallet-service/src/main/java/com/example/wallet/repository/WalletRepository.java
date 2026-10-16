package com.example.wallet.repository;

import com.example.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository = data-access layer.
 *
 * What is JpaRepository<Wallet, Long>?
 * ------------------------------------
 * Spring Data JPA generates an implementation at runtime.
 * Wallet = entity type
 * Long   = type of the primary key (id)
 *
 * What do we get for free?
 * - save(entity)
 * - findById(id)
 * - findAll()
 * - deleteById(id)
 * - count()
 * ...and more
 *
 * What problem does it solve?
 * Without it, we would write JDBC/SQL boilerplate ourselves.
 *
 * Method naming:
 * findByUserId → Spring parses the method name and generates:
 *   SELECT * FROM wallet WHERE user_id = ?
 */
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserId(Long userId);
}
