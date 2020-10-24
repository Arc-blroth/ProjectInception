package ai.arcblroth.projectInception.client.taterwebz;

import ai.arcblroth.projectInception.client.ForceRenderedScreen;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.text.TranslatableText;

public class OpenJ9WarningScreen extends ConfirmScreen implements ForceRenderedScreen {

    public OpenJ9WarningScreen(BooleanConsumer callback) {
        super(callback,
                new TranslatableText("gui.project_inception.openj9_warning.title"),
                new TranslatableText("gui.project_inception.openj9_warning.desc"),
                ScreenTexts.PROCEED,
                ScreenTexts.CANCEL
        );
    }
}
