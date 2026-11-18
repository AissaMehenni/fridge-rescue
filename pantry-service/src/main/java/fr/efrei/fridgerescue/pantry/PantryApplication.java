package fr.efrei.fridgerescue.pantry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PantryApplication {
    public static void main(String[] args) {
        SpringApplication.run(PantryApplication.class, args);
    }
}
