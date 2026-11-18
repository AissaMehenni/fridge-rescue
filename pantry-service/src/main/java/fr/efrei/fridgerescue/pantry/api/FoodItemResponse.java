package fr.efrei.fridgerescue.pantry.api;

import fr.efrei.fridgerescue.pantry.domain.Category;
import fr.efrei.fridgerescue.pantry.domain.FoodItem;
import fr.efrei.fridgerescue.pantry.domain.Freshness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record FoodItemResponse(
        UUID id,
        String name,
        BigDecimal quantity,
        String unit,
        Category category,
        LocalDate expiresOn,
        long daysLeft,
        Freshness freshness) {

    public static FoodItemResponse of(FoodItem item, LocalDate today) {
        long daysLeft = ChronoUnit.DAYS.between(today, item.getExpiresOn());
        return new FoodItemResponse(item.getId(), item.getName(), item.getQuantity(), item.getUnit(),
                item.getCategory(), item.getExpiresOn(), daysLeft, Freshness.of(daysLeft));
    }
}
