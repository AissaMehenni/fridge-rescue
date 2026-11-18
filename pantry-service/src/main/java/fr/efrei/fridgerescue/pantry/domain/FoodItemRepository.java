package fr.efrei.fridgerescue.pantry.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FoodItemRepository extends JpaRepository<FoodItem, UUID> {

    List<FoodItem> findAllByOrderByExpiresOnAscNameAsc();

    List<FoodItem> findByExpiresOnLessThanEqualOrderByExpiresOnAsc(LocalDate limit);
}
