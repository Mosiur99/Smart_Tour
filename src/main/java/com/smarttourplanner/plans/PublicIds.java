package com.smarttourplanner.plans;

import java.security.SecureRandom;

/** Random share IDs, so plans can't be found by counting. */
final class PublicIds {

    private static final String ALPHABET = "23456789abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PublicIds() {
    }

    static String next() {
        StringBuilder id = new StringBuilder(LENGTH);
        for (int index = 0; index < LENGTH; index++) {
            id.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return id.toString();
    }
}
