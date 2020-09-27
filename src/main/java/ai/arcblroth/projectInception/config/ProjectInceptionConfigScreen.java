package ai.arcblroth.projectInception.config;

import ai.arcblroth.projectInception.ProjectInception;
import com.google.common.collect.ImmutableList;
import io.github.prospector.modmenu.api.ConfigScreenFactory;
import io.github.prospector.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import java.util.Optional;

public class ProjectInceptionConfigScreen implements ModMenuApi {

    @Override
    public String getModId() {
        return ProjectInception.MODID;
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (parent) -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(new TranslatableText("config.projectInception.title"))
                    .setAlwaysShowTabs(false)
                    .transparentBackground()
                    .setShouldListSmoothScroll(true);
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            ConfigCategory inception = builder.getOrCreateCategory(new TranslatableText("config.projectInception.category.inception"));
            inception.addEntry(
                    buildDPIEntry(entryBuilder, ProjectInceptionConfig.DISPLAY_SCALE, 64,"inception.dpi", 3)
                        .setSaveConsumer(newVal -> ProjectInceptionConfig.DISPLAY_SCALE = newVal)
                        .build()
            );
            inception.addEntry(
                    entryBuilder.startStrField(
                            new TranslatableText("config.projectInception.inception.extra_vm_args"),
                            ProjectInceptionConfig.INCEPTION_EXTRA_VM_ARGS
                    )
                            .setTooltip(getTooltip("inception.extra_vm_args", 1))
                            .setDefaultValue("")
                            .setSaveConsumer(newVal -> ProjectInceptionConfig.INCEPTION_EXTRA_VM_ARGS = newVal)
                            .build()
            );
            IntegratedServer server = MinecraftClient.getInstance().getServer();
            if(FabricLoader.getInstance().isModLoaded("techreborn") && server != null) {
                inception.addEntry(
                        entryBuilder.startBooleanToggle(
                                new TranslatableText("config.projectInception.inception.use_techreborn_recipes"),
                                ProjectInceptionConfig.USE_TECHREBORN_RECIPES
                        )
                                .setTooltip(getTooltip("inception.use_techreborn_recipes", 3))
                                .setDefaultValue(false)
                                .setSaveConsumer(newVal -> {
                                    ProjectInceptionConfig.USE_TECHREBORN_RECIPES = newVal;
                                    server.getCommandManager().execute(server.getCommandSource(), "reload");
                                })
                                .build()
                );
            }
            inception.addEntry(
                    entryBuilder.startBooleanToggle(
                            new TranslatableText("config.projectInception.inception.use_faux_inner"),
                            ProjectInceptionConfig.USE_FAUX_INNER
                    )
                            .setTooltip(getTooltip("inception.use_faux_inner", 3))
                            .setDefaultValue(false)
                            .setSaveConsumer(newVal -> ProjectInceptionConfig.USE_FAUX_INNER = newVal)
                            .build()
            );

            ConfigCategory taterwebz = builder.getOrCreateCategory(new TranslatableText("config.projectInception.category.taterwebz"));
            taterwebz.addEntry(
                    buildDPIEntry(entryBuilder, ProjectInceptionConfig.TATERWEBZ_SCALE, 128,"taterwebz.dpi", 3)
                            .setSaveConsumer(newVal -> ProjectInceptionConfig.TATERWEBZ_SCALE = newVal)
                            .build()
            );
            taterwebz.addEntry(
                    entryBuilder.startStrField(
                            new TranslatableText("config.projectInception.taterwebz.home_page"),
                            ProjectInceptionConfig.TATERWEBZ_HOME_PAGE
                    )
                            .setTooltip(getTooltip("taterwebz.home_page", 1))
                            .setDefaultValue("https://google.com/")
                            .setSaveConsumer(newVal -> ProjectInceptionConfig.TATERWEBZ_HOME_PAGE = newVal)
                            .build()
            );

            builder.setSavingRunnable(ProjectInceptionConfig::save);
            return builder.build();
        };
    }

    private static DropdownMenuBuilder<Integer> buildDPIEntry(ConfigEntryBuilder entryBuilder, int top, int defaultVal, String translationKey, int tooltipLines) {
        return  entryBuilder.startDropdownMenu(
                    new TranslatableText("config.projectInception." + translationKey),
                    top,
                    ProjectInceptionConfigScreen::stringToInt
                )
                .setSelections(ImmutableList.of(8, 16, 32, 64, 128, 256, 512))
                .setDefaultValue(defaultVal)
                .setTooltip(getTooltip(translationKey, tooltipLines));
    }

    private static Integer stringToInt(String in) {
        try {
            return Integer.parseInt(in);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Optional<Text[]> getTooltip(String optionKey, int lines) {
        if(lines == 1) {
            return Optional.of(new Text[] { new TranslatableText("config.projectInception." + optionKey + ".tooltip") });
        }
        String prefix = "config.projectInception." + optionKey + ".tooltip_";
        Text[] out = new Text[lines];
        for(int i = 1; i <= lines; i++) {
            out[i - 1] = new TranslatableText(prefix + i);
        }
        return Optional.of(out);
    }

}
