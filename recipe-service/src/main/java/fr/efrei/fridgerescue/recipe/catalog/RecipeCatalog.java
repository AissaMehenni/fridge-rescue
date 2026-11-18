package fr.efrei.fridgerescue.recipe.catalog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/** Catalogue de recettes en lecture seule, chargé au démarrage depuis le classpath. */
@Component
public class RecipeCatalog {

    private final List<RecipeEntry> recipes;

    @Autowired
    public RecipeCatalog(ObjectMapper mapper) {
        try (InputStream in = new ClassPathResource("recipes.json").getInputStream()) {
            this.recipes = List.copyOf(mapper.readValue(in, new TypeReference<List<RecipeEntry>>() { }));
        } catch (IOException e) {
            throw new UncheckedIOException("Catalogue de recettes illisible", e);
        }
    }

    /** Pour les tests unitaires. */
    public RecipeCatalog(List<RecipeEntry> recipes) {
        this.recipes = List.copyOf(recipes);
    }

    public List<RecipeEntry> all() {
        return recipes;
    }
}
