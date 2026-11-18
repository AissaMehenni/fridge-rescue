package fr.efrei.fridgerescue.pantry.config;

import fr.efrei.fridgerescue.pantry.domain.Category;
import fr.efrei.fridgerescue.pantry.domain.FoodItem;
import fr.efrei.fridgerescue.pantry.domain.FoodItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Remplit un frigo de démonstration si la base est vide. Les dates sont relatives
 * à aujourd'hui pour que la démo montre toujours des aliments urgents.
 */
@Component
@ConditionalOnProperty(name = "fridge.demo-data", havingValue = "true")
class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final FoodItemRepository repository;
    private final Clock clock;

    DemoDataLoader(FoodItemRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now(clock);
        List<FoodItem> items = List.of(
                new FoodItem("Tomate", new BigDecimal("4"), "pcs", Category.VEGETABLE, today.plusDays(1)),
                new FoodItem("Courgette", new BigDecimal("2"), "pcs", Category.VEGETABLE, today.plusDays(2)),
                new FoodItem("Oeuf", new BigDecimal("6"), "pcs", Category.DAIRY, today.plusDays(5)),
                new FoodItem("Lait", new BigDecimal("1"), "L", Category.DAIRY, today),
                new FoodItem("Banane", new BigDecimal("3"), "pcs", Category.FRUIT, today.plusDays(1)),
                new FoodItem("Poulet", new BigDecimal("400"), "g", Category.MEAT, today.plusDays(2)),
                new FoodItem("Riz", new BigDecimal("500"), "g", Category.GRAIN, today.plusDays(180)),
                new FoodItem("Fromage râpé", new BigDecimal("150"), "g", Category.DAIRY, today.plusDays(7)),
                new FoodItem("Épinards", new BigDecimal("200"), "g", Category.VEGETABLE, today.minusDays(1)));
        repository.saveAll(items);
        log.info("Frigo de démonstration chargé ({} aliments)", items.size());
    }
}
