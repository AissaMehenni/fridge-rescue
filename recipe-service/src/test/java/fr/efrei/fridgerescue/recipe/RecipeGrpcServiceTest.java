package fr.efrei.fridgerescue.recipe;

import fr.efrei.fridgerescue.recipe.grpc.RecipeGrpcService;
import fr.efrei.fridgerescue.recipe.v1.*;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Contexte Spring complet + vrai appel gRPC via un transport in-process. */
@SpringBootTest(properties = {"spring.grpc.server.port=0", "server.port=0"})
class RecipeGrpcServiceTest {

    @Autowired
    RecipeGrpcService recipeGrpcService;

    private Server server;
    private ManagedChannel channel;
    private RecipeServiceGrpc.RecipeServiceBlockingStub stub;

    @BeforeEach
    void start() throws Exception {
        String name = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(name).directExecutor().addService(recipeGrpcService).build().start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        stub = RecipeServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void stop() throws Exception {
        channel.shutdownNow();
        server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void listsCatalog() {
        assertThat(stub.listRecipes(ListRecipesRequest.getDefaultInstance()).getRecipesCount()).isGreaterThanOrEqualTo(10);
    }

    @Test
    void suggestsRecipesForAnUrgentIngredient() {
        SuggestRecipesResponse response = stub.suggestRecipes(SuggestRecipesRequest.newBuilder()
                .addIngredients(Ingredient.newBuilder().setName("Banane").setDaysLeft(0))
                .addIngredients(Ingredient.newBuilder().setName("Lait").setDaysLeft(3))
                .setLimit(3).build());

        assertThat(response.getSuggestionsList()).isNotEmpty().hasSizeLessThanOrEqualTo(3);
        RecipeSuggestion best = response.getSuggestions(0);
        assertThat(best.getRecipe().getId()).isEqualTo("smoothie-banane");
        assertThat(best.getRescuedIngredientsList()).contains("Banane");
    }

    @Test
    void rejectsInvalidLimit() {
        assertThatThrownBy(() -> stub.suggestRecipes(SuggestRecipesRequest.newBuilder().setLimit(500).build()))
                .isInstanceOfSatisfying(StatusRuntimeException.class,
                        e -> assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT));
    }
}
