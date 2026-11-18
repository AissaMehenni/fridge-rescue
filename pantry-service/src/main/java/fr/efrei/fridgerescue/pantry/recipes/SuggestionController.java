package fr.efrei.fridgerescue.pantry.recipes;

import fr.efrei.fridgerescue.pantry.api.FoodItemResponse;
import fr.efrei.fridgerescue.pantry.service.PantryService;
import fr.efrei.fridgerescue.pantry.recipes.SuggestionView.*;
import fr.efrei.fridgerescue.recipe.v1.Ingredient;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Façade REST : agrège le frigo (local) et le moteur de recettes (gRPC). */
@RestController
@Validated
public class SuggestionController {

    private final PantryService pantry;
    private final RecipeClient recipes;

    public SuggestionController(PantryService pantry, RecipeClient recipes) {
        this.pantry = pantry;
        this.recipes = recipes;
    }

    @GetMapping("/api/suggestions")
    public SuggestionsResponse suggestions(@RequestParam(defaultValue = "6") @Min(1) @Max(20) int limit) {
        List<Ingredient> ingredients = pantry.list().stream()
                .filter(item -> item.daysLeft() >= 0)
                .map(SuggestionController::toIngredient)
                .toList();
        return recipes.suggest(ingredients, limit)
                .map(list -> new SuggestionsResponse(true, list.stream().map(SuggestionDto::of).toList()))
                .orElseGet(() -> new SuggestionsResponse(false, List.of()));
    }

    @GetMapping("/api/recipes")
    public RecipesResponse catalog() {
        return recipes.listRecipes()
                .map(list -> new RecipesResponse(true, list.stream().map(RecipeDto::of).toList()))
                .orElseGet(() -> new RecipesResponse(false, List.of()));
    }

    private static Ingredient toIngredient(FoodItemResponse item) {
        return Ingredient.newBuilder()
                .setName(item.name())
                .setDaysLeft((int) Math.min(Integer.MAX_VALUE, item.daysLeft()))
                .build();
    }
}
