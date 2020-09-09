package ai.arcblroth.projectInception.config;

import ai.arcblroth.projectInception.ProjectInception;
import com.google.common.collect.ImmutableList;
import io.github.prospector.modmenu.api.ConfigScreenFactory;
import io.github.prospector.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import net.minecraft.text.TranslatableText;

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
                    buildDPIEntry(entryBuilder, ProjectInceptionConfig.DISPLAY_SCALE, 64,"config.projectInception.inception.dpi")
                        .setSaveConsumer(newVal -> ProjectInceptionConfig.DISPLAY_SCALE = newVal)
                        .build()
            );
            inception.addEntry(
                    entryBuilder.startStrField(
                            new TranslatableText("config.projectInception.inception.extra_vm_args"),
                            ProjectInceptionConfig.INCEPTION_EXTRA_VM_ARGS
                    )
                            .setTooltip(new TranslatableText("config.projectInception.inception.extra_vm_args.tooltip"))
                            .setDefaultValue("")
                            .setSaveConsumer(newVal -> ProjectInceptionConfig.INCEPTION_EXTRA_VM_ARGS = newVal)
                            .build()
            );
            inception.addEntry(
                    entryBuilder.startBooleanToggle(
                            new TranslatableText("config.projectInception.inception.use_faux_inner"),
                            ProjectInceptionConfig.USE_FAUX_INNER
                    )
                            .setTooltip(new TranslatableText("config.projectInception.inception.use_faux_inner.tooltip"))
                            .setDefaultValue(false)
                            .setSaveConsumer(newVal -> ProjectInceptionConfig.USE_FAUX_INNER = newVal)
                            .build()
            );

            ConfigCategory taterwebz = builder.getOrCreateCategory(new TranslatableText("config.projectInception.category.taterwebz"));
            taterwebz.addEntry(
                    buildDPIEntry(entryBuilder, ProjectInceptionConfig.TATERWEBZ_SCALE, 128,"config.projectInception.taterwebz.dpi")
                            .setSaveConsumer(newVal -> ProjectInceptionConfig.TATERWEBZ_SCALE = newVal)
                            .build()
            );
            taterwebz.addEntry(
                    entryBuilder.startStrField(
                            new TranslatableText("config.projectInception.taterwebz.home_page"),
                            ProjectInceptionConfig.TATERWEBZ_HOME_PAGE
                    )
                            .setTooltip(new TranslatableText("config.projectInception.taterwebz.home_page.tooltip"))
                            .setDefaultValue("https://google.com/")
                            .setSaveConsumer(newVal -> ProjectInceptionConfig.TATERWEBZ_HOME_PAGE = newVal)
                            .build()
            );

            builder.setSavingRunnable(ProjectInceptionConfig::save);
            return builder.build();
        };
    }

    private static DropdownMenuBuilder<Integer> buildDPIEntry(ConfigEntryBuilder entryBuilder, int top, int defaultVal, String translationKey) {
        return  entryBuilder.startDropdownMenu(
                    new TranslatableText(translationKey),
                    top,
                    ProjectInceptionConfigScreen::stringToInt
                )
                .setSelections(ImmutableList.of(8, 16, 32, 64, 128, 256, 512))
                .setDefaultValue(defaultVal)
                .setTooltip(new TranslatableText(translationKey + ".tooltip"));
    }

    private static Integer stringToInt(String in) {
        try {
            return Integer.parseInt(in);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
