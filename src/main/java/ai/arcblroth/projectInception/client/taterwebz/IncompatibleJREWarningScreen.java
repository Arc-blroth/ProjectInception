package ai.arcblroth.projectInception.client.taterwebz;

import ai.arcblroth.projectInception.client.ForceRenderedScreen;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

public class IncompatibleJREWarningScreen extends ConfirmScreen implements ForceRenderedScreen {

    private static final String LIBAWT_BUG_LINK = "https://bugs.launchpad.net/ubuntu/+source/openjdk-lts/+bug/1838740";

    public IncompatibleJREWarningScreen(BooleanConsumer callback) {
        super(callback,
                new TranslatableText("gui.project_inception.incompatible_jre.title"),
                new TranslatableText("gui.project_inception.incompatible_jre.desc_1")
                        .append(new LiteralText(LIBAWT_BUG_LINK).styled(s -> s.withFormatting(Formatting.AQUA)))
                        .append(new TranslatableText("gui.project_inception.incompatible_jre.desc_2")),
                ScreenTexts.PROCEED,
                ScreenTexts.CANCEL
        );
    }
}
