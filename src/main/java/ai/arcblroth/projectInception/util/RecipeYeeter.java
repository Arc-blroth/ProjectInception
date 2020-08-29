package ai.arcblroth.projectInception.util;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;

// Because adding cotton for the equivalent of 1 mixin was a bad idea
public class RecipeYeeter {

    private static final ArrayList<Identifier> recipesToYeet = new ArrayList<>();

    private RecipeYeeter() {}

    public static void yeetRecipe(Identifier identifier) {
        recipesToYeet.add(identifier);
    }

    public static Collection<Identifier> getRecipesForYeeting() {
        return recipesToYeet;
    }

}
