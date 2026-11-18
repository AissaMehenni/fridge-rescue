package fr.efrei.fridgerescue.pantry.config;

import fr.efrei.fridgerescue.pantry.domain.Category;
import fr.efrei.fridgerescue.pantry.domain.FoodItem;
import fr.efrei.fridgerescue.pantry.domain.FoodItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Remplit un frigo de démonstration au tout premier démarrage. Les dates sont relatives
 * à aujourd'hui pour que la démo montre toujours des aliments urgents.
 * <p>
 * Concurrence : l'insertion du marqueur {@code demo_seed(id=1)} et des aliments se fait
 * dans une seule transaction ; une seconde réplique bute sur la clé primaire et abandonne.
 */
@Component
@ConditionalOnProperty(name = "fridge.demo-data", havingValue = "true")
class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final FoodItemRepository repository;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final Clock clock;

    DemoDataLoader(FoodItemRepository repository, JdbcTemplate jdbc, TransactionTemplate tx, Clock clock) {
        this.repository = repository;
        this.jdbc = jdbc;
        this.tx = tx;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        tx.executeWithoutResult(status -> {
            try {
                jdbc.update("INSERT INTO demo_seed (id, seeded_at) VALUES (1, ?)", Timestamp.from(clock.instant()));
            } catch (DuplicateKeyException alreadySeeded) {
                status.setRollbackOnly();
                return;
            }
            seed();
        });
    }

    private void seed() {
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
