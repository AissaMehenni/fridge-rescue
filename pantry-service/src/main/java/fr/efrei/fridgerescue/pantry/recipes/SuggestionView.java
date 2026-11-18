package fr.efrei.fridgerescue.pantry.recipes;

import fr.efrei.fridgerescue.recipe.v1.Recipe;
import fr.efrei.fridgerescue.recipe.v1.RecipeSuggestion;

import java.util.List;

/** Représentations JSON exposées au front (découplées des messages protobuf). */
public final class SuggestionView {

    private SuggestionView() {
    }

    public record RecipeDto(String id, String title, String emoji, int prepMinutes,
                            List<String> ingredients, List<String> steps) {
        static RecipeDto of(Recipe r) {
            return new RecipeDto(r.getId(), r.getTitle(), r.getEmoji(), r.getPrepMinutes(),
                    List.copyOf(r.getIngredientsList()), List.copyOf(r.getStepsList()));
        }
    }

    public record SuggestionDto(RecipeDto recipe, int score, List<String> usedIngredients,
                                List<String> missingIngredients, List<String> rescuedIngredients) {
        static SuggestionDto of(RecipeSuggestion s) {
            return new SuggestionDto(RecipeDto.of(s.getRecipe()), s.getScore(),
                    List.copyOf(s.getUsedIngredientsList()), List.copyOf(s.getMissingIngredientsList()),
                    List.copyOf(s.getRescuedIngredientsList()));
        }
    }

    /** {@code available=false} : recipe-service injoignable (circuit ouvert, panne…). */
    public record SuggestionsResponse(boolean available, List<SuggestionDto> suggestions) {
    }

    public record RecipesResponse(boolean available, List<RecipeDto> recipes) {
    }
}
