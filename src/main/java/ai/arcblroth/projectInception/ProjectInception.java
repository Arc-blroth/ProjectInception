package ai.arcblroth.projectInception;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProjectInception implements ModInitializer {

	public static final String MODID = "project_inception";
	public static final Logger LOGGER = LogManager.getLogger();

	@Override
	public void onInitialize() {
		//if(System.getProperty("projectInceptionInner").equals("true")) {
		LOGGER.log(Level.INFO, "Initializing for inner instance...");
		//} else {
		//	LOGGER.log(Level.INFO, "Initializing for parent instance...");
		//}
	}

}
