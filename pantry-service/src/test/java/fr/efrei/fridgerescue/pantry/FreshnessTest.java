package fr.efrei.fridgerescue.pantry;

import fr.efrei.fridgerescue.pantry.domain.Freshness;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class FreshnessTest {

    @ParameterizedTest
    @CsvSource({"-3,EXPIRED", "-1,EXPIRED", "0,URGENT", "1,URGENT", "2,SOON", "4,SOON", "5,FRESH", "90,FRESH"})
    void classifiesByDaysLeft(long daysLeft, Freshness expected) {
        assertThat(Freshness.of(daysLeft)).isEqualTo(expected);
    }
}
