package fr.efrei.fridgerescue.recipe;

import fr.efrei.fridgerescue.recipe.catalog.IngredientNames;
import fr.efrei.fridgerescue.recipe.catalog.RecipeCatalog;
import fr.efrei.fridgerescue.recipe.catalog.RecipeEntry;
import fr.efrei.fridgerescue.recipe.catalog.SuggestionEngine;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SuggestionEngineTest {

    private final SuggestionEngine engine = new SuggestionEngine(new RecipeCatalog(List.of(
            new RecipeEntry("omelette", "Omelette", "🍳", 10, List.of("Oeuf", "Épinards"), List.of()),
            new RecipeEntry("crepes", "Crêpes", "🥞", 20, List.of("Farine", "Lait", "Oeuf", "Beurre"), List.of()),
            new RecipeEntry("salade", "Salade", "🥗", 5, List.of("Tomate", "Basilic"), List.of()))));

    @Test
    void normalizesAccentsCaseAndPlurals() {
        assertThat(IngredientNames.normalize(" ÉPINARDS ")).isEqualTo("epinard");
        assertThat(IngredientNames.normalize("Fromage râpé")).isEqualTo("fromage rape");
        assertThat(IngredientNames.normalize("Oeufs")).isEqualTo("oeuf");
    }

    @Test
    void ranksRecipesThatRescueUrgentIngredientsFirst() {
        var result = engine.suggest(Map.of("epinards", 0, "oeufs", 6, "lait", 10), 0);

        assertThat(result).extracting(s -> s.recipe().id()).containsExactly("omelette", "crepes");
        var omelette = result.get(0);
        assertThat(omelette.score()).isEqualTo(100);          // couverture 1, sauvetage ≥ 1
        assertThat(omelette.rescued()).containsExactly("Épinards");
        assertThat(result.get(1).missing()).containsExactly("Farine", "Beurre");
    }

    @Test
    void ignoresExpiredIngredientsAndRecipesWithoutMatch() {
        var result = engine.suggest(Map.of("Tomate", -1, "Oeuf", 3), 0);
        assertThat(result).extracting(s -> s.recipe().id()).doesNotContain("salade");
    }

    @Test
    void respectsLimit() {
        assertThat(engine.suggest(Map.of("Oeuf", 1), 1)).hasSize(1);
    }
}
