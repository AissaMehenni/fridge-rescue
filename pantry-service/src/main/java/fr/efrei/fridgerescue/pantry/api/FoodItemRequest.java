package fr.efrei.fridgerescue.pantry.api;

import fr.efrei.fridgerescue.pantry.domain.Category;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Corps des requêtes POST / PUT. */
public record FoodItemRequest(
        @NotBlank @Size(max = 80) String name,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2) BigDecimal quantity,
        @NotBlank @Size(max = 16) String unit,
        @NotNull Category category,
        @NotNull LocalDate expiresOn) {
}
