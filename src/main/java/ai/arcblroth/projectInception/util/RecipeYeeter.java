package ai.arcblroth.projectInception.util;

import ai.arcblroth.projectInception.config.ProjectInceptionConfig;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

// Because adding cotton for the equivalent of 1 mixin was a bad idea
public class RecipeYeeter {

    public static final Supplier<Boolean> YES = () -> true;
    public static final Supplier<Boolean> USE_TECHREBORN_RECIPES = () -> ProjectInceptionConfig.USE_TECHREBORN_RECIPES;
    public static final Supplier<Boolean> DONT_USE_TECHREBORN_RECIPES = () -> !ProjectInceptionConfig.USE_TECHREBORN_RECIPES;
    private static final HashMap<Identifier, Supplier<Boolean>> recipesToYeet = new HashMap<>();

    private RecipeYeeter() {}

    public static void yeetRecipe(Supplier<Boolean> whenThisIsTrue, Identifier identifier) {
        recipesToYeet.put(identifier, whenThisIsTrue);
    }

    public static void yeetRecipe(Identifier identifier) {
        recipesToYeet.put(identifier, YES);
    }

    public static Collection<Identifier> getRecipesForYeeting() {
        return recipesToYeet.entrySet().stream().filter(e -> e.getValue().get()).map(Map.Entry::getKey).collect(Collectors.toSet());
    }

}
