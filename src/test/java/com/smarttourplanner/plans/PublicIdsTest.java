package com.smarttourplanner.plans;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PublicIdsTest {

    @Test
    void createsTwelveCharacterIdsWithoutConfusingLetters() {
        assertThat(PublicIds.next()).hasSize(12).matches("[2-9a-zA-Z]+").doesNotContain("0", "1", "l", "O", "I");
    }

    @Test
    void doesNotRepeat() {
        Set<String> ids = new HashSet<>();
        for (int count = 0; count < 10_000; count++) {
            ids.add(PublicIds.next());
        }

        assertThat(ids).hasSize(10_000);
    }
}
