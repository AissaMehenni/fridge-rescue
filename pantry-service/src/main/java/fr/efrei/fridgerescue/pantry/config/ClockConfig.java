package fr.efrei.fridgerescue.pantry.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class ClockConfig {
    /** Horloge injectable : les tests peuvent figer la date du jour. */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
