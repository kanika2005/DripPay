package com.drippay.util;

import java.security.SecureRandom;

public final class WalletIdGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private WalletIdGenerator() {
    }

    public static String generate() {
        StringBuilder sb = new StringBuilder("DRIP");
        for (int i = 0; i < 8; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
