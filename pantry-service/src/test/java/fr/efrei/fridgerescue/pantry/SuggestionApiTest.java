package fr.efrei.fridgerescue.pantry;

import fr.efrei.fridgerescue.pantry.recipes.RecipeClient;
import fr.efrei.fridgerescue.recipe.v1.Ingredient;
import fr.efrei.fridgerescue.recipe.v1.Recipe;
import fr.efrei.fridgerescue.recipe.v1.RecipeSuggestion;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Le client gRPC est simulé : on teste l'agrégation REST et le mode dégradé. */
@SpringBootTest
@AutoConfigureMockMvc
class SuggestionApiTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RecipeClient recipeClient;

    @Test
    @SuppressWarnings("unchecked")
    void sendsOnlyNonExpiredItemsAndMapsSuggestions() throws Exception {
        RecipeSuggestion suggestion = RecipeSuggestion.newBuilder()
                .setRecipe(Recipe.newBuilder().setId("omelette").setTitle("Omelette").setEmoji("🍳").setPrepMinutes(10))
                .setScore(92).addRescuedIngredients("Épinards").build();
        when(recipeClient.suggest(anyList(), anyInt())).thenReturn(Optional.of(List.of(suggestion)));

        mvc.perform(get("/api/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.suggestions[0].recipe.title").value("Omelette"))
                .andExpect(jsonPath("$.suggestions[0].score").value(92));

        ArgumentCaptor<List<Ingredient>> sent = ArgumentCaptor.forClass(List.class);
        verify(recipeClient).suggest(sent.capture(), anyInt());
        // Le frigo de démo contient des épinards périmés : ils ne doivent pas être envoyés.
        assertThat(sent.getValue()).extracting(Ingredient::getName).isNotEmpty().doesNotContain("Épinards");
        assertThat(sent.getValue()).allMatch(i -> i.getDaysLeft() >= 0);
    }

    @Test
    void degradesGracefullyWhenRecipeServiceIsDown() throws Exception {
        when(recipeClient.suggest(anyList(), anyInt())).thenReturn(Optional.empty());

        mvc.perform(get("/api/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.suggestions").isEmpty());
    }
}
