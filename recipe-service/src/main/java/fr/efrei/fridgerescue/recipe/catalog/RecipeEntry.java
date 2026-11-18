package fr.efrei.fridgerescue.recipe.catalog;

import java.util.List;

/** Recette telle que décrite dans recipes.json. */
public record RecipeEntry(String id, String title, String emoji, int prepMinutes,
                          List<String> ingredients, List<String> steps) {
}
