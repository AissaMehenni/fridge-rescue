package fr.efrei.fridgerescue.recipe.grpc;

import fr.efrei.fridgerescue.recipe.catalog.RecipeCatalog;
import fr.efrei.fridgerescue.recipe.catalog.RecipeEntry;
import fr.efrei.fridgerescue.recipe.catalog.SuggestionEngine;
import fr.efrei.fridgerescue.recipe.v1.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/** Implémentation serveur du contrat recipe.v1 (enregistrée automatiquement par spring-grpc). */
@Service
public class RecipeGrpcService extends RecipeServiceGrpc.RecipeServiceImplBase {

    static final int MAX_LIMIT = 50;

    private final SuggestionEngine engine;
    private final RecipeCatalog catalog;

    public RecipeGrpcService(SuggestionEngine engine, RecipeCatalog catalog) {
        this.engine = engine;
        this.catalog = catalog;
    }

    @Override
    public void suggestRecipes(SuggestRecipesRequest request, StreamObserver<SuggestRecipesResponse> response) {
        if (request.getLimit() < 0 || request.getLimit() > MAX_LIMIT) {
            response.onError(Status.INVALID_ARGUMENT
                    .withDescription("limit doit être entre 0 et " + MAX_LIMIT).asRuntimeException());
            return;
        }
        Map<String, Integer> pantry = new LinkedHashMap<>();
        request.getIngredientsList().forEach(i -> pantry.merge(i.getName(), i.getDaysLeft(), Math::min));

        SuggestRecipesResponse.Builder out = SuggestRecipesResponse.newBuilder();
        engine.suggest(pantry, request.getLimit()).forEach(s -> out.addSuggestions(RecipeSuggestion.newBuilder()
                .setRecipe(toProto(s.recipe()))
                .setScore(s.score())
                .addAllUsedIngredients(s.used())
                .addAllMissingIngredients(s.missing())
                .addAllRescuedIngredients(s.rescued())));
        response.onNext(out.build());
        response.onCompleted();
    }

    @Override
    public void listRecipes(ListRecipesRequest request, StreamObserver<ListRecipesResponse> response) {
        ListRecipesResponse.Builder out = ListRecipesResponse.newBuilder();
        catalog.all().forEach(r -> out.addRecipes(toProto(r)));
        response.onNext(out.build());
        response.onCompleted();
    }

    private static Recipe toProto(RecipeEntry r) {
        return Recipe.newBuilder()
                .setId(r.id()).setTitle(r.title()).setEmoji(r.emoji()).setPrepMinutes(r.prepMinutes())
                .addAllIngredients(r.ingredients()).addAllSteps(r.steps())
                .build();
    }
}
