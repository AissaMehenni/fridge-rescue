package fr.efrei.fridgerescue.recipe.catalog;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Classe les recettes pour « sauver » d'abord ce qui périme bientôt.
 *
 * <pre>
 * urgence(j)  = 1 / (1 + j)          j = jours restants (aujourd'hui = 1.0, demain = 0.5…)
 * couverture  = ingrédients présents / ingrédients de la recette
 * sauvetage   = min(1, Σ urgence des ingrédients présents)
 * score       = 100 × (0.5 × couverture + 0.5 × sauvetage)
 * </pre>
 * Une recette sans aucun ingrédient du frigo n'est jamais proposée.
 */
@Component
public class SuggestionEngine {

    static final int DEFAULT_LIMIT = 6;
    static final int RESCUE_THRESHOLD_DAYS = 2;

    public record Suggestion(RecipeEntry recipe, int score, List<String> used,
                             List<String> missing, List<String> rescued) {
    }

    private final RecipeCatalog catalog;

    public SuggestionEngine(RecipeCatalog catalog) {
        this.catalog = catalog;
    }

    /** @param pantry nom brut de l'ingrédient → jours restants (les périmés sont ignorés) */
    public List<Suggestion> suggest(Map<String, Integer> pantry, int limit) {
        Map<String, Integer> available = new HashMap<>();
        pantry.forEach((name, days) -> {
            if (days != null && days >= 0) {
                available.merge(IngredientNames.normalize(name), days, Math::min);
            }
        });

        List<Suggestion> result = new ArrayList<>();
        for (RecipeEntry recipe : catalog.all()) {
            List<String> used = new ArrayList<>();
            List<String> missing = new ArrayList<>();
            List<String> rescued = new ArrayList<>();
            double rescuePoints = 0;
            for (String ingredient : recipe.ingredients()) {
                Integer days = available.get(IngredientNames.normalize(ingredient));
                if (days == null) {
                    missing.add(ingredient);
                    continue;
                }
                used.add(ingredient);
                rescuePoints += 1.0 / (1 + days);
                if (days <= RESCUE_THRESHOLD_DAYS) {
                    rescued.add(ingredient);
                }
            }
            if (used.isEmpty()) {
                continue;
            }
            double coverage = (double) used.size() / recipe.ingredients().size();
            int score = (int) Math.round(100 * (0.5 * coverage + 0.5 * Math.min(1.0, rescuePoints)));
            result.add(new Suggestion(recipe, score, used, missing, rescued));
        }

        result.sort(Comparator.comparingInt(Suggestion::score).reversed()
                .thenComparingInt(s -> s.missing().size())
                .thenComparing(s -> s.recipe().title()));
        return result.subList(0, Math.min(limit > 0 ? limit : DEFAULT_LIMIT, result.size()));
    }
}
