package fr.efrei.fridgerescue.recipe.catalog;

import java.text.Normalizer;
import java.util.Locale;

/** Normalise un nom d'ingrédient pour comparer "Épinards" et "epinard". */
public final class IngredientNames {

    private IngredientNames() {
    }

    public static String normalize(String raw) {
        String s = Normalizer.normalize(raw.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")      // accents
                .replaceAll("[^a-z ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        // pluriel simple, mot par mot ("tomates cerises" -> "tomate cerise")
        StringBuilder out = new StringBuilder();
        for (String word : s.split(" ")) {
            if (word.length() > 3 && (word.endsWith("s") || word.endsWith("x"))) {
                word = word.substring(0, word.length() - 1);
            }
            out.append(out.isEmpty() ? "" : " ").append(word);
        }
        return out.toString();
    }
}
