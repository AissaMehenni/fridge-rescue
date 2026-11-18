package fr.efrei.fridgerescue.pantry.recipes;

import fr.efrei.fridgerescue.recipe.v1.*;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Client gRPC vers recipe-service. Le canal "recipes" est configuré dans application.yml
 * (spring.grpc.client.channels.recipes.address). En clair côté application : c'est le
 * sidecar Istio qui chiffre en mTLS.
 */
@Component
public class RecipeClient {

    private static final Logger log = LoggerFactory.getLogger(RecipeClient.class);

    private final RecipeServiceGrpc.RecipeServiceBlockingStub stub;
    private final Duration deadline;

    public RecipeClient(GrpcChannelFactory channels,
                        @Value("${fridge.recipes.deadline:2s}") Duration deadline) {
        this.stub = RecipeServiceGrpc.newBlockingStub(channels.createChannel("recipes"));
        this.deadline = deadline;
    }

    /** Renvoie vide si recipe-service est indisponible : l'appelant passe en mode dégradé. */
    public Optional<List<RecipeSuggestion>> suggest(List<Ingredient> ingredients, int limit) {
        try {
            SuggestRecipesResponse response = withDeadline().suggestRecipes(SuggestRecipesRequest.newBuilder()
                    .addAllIngredients(ingredients).setLimit(limit).build());
            return Optional.of(response.getSuggestionsList());
        } catch (StatusRuntimeException e) {
            log.warn("recipe-service indisponible ({}): mode dégradé", e.getStatus().getCode());
            return Optional.empty();
        }
    }

    public Optional<List<Recipe>> listRecipes() {
        try {
            return Optional.of(withDeadline().listRecipes(ListRecipesRequest.getDefaultInstance()).getRecipesList());
        } catch (StatusRuntimeException e) {
            log.warn("recipe-service indisponible ({}): mode dégradé", e.getStatus().getCode());
            return Optional.empty();
        }
    }

    private RecipeServiceGrpc.RecipeServiceBlockingStub withDeadline() {
        return stub.withDeadlineAfter(deadline.toMillis(), TimeUnit.MILLISECONDS);
    }
}
