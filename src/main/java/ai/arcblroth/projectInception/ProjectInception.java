package ai.arcblroth.projectInception;

import net.fabricmc.api.ModInitializer;
import net.openhft.chronicle.queue.ChronicleQueue;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProjectInception implements ModInitializer {

	public static final String MODID = "project_inception";
	public static final Logger LOGGER = LogManager.getLogger();
	public static final boolean IS_INNER = System.getProperty("projectInceptionInner") != null
			&& System.getProperty("projectInceptionInner").equals("true");

	public static ChronicleQueue queue;

    @Override
	public void onInitialize() {

	}

}
