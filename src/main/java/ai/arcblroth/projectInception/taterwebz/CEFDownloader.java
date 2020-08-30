package ai.arcblroth.projectInception.taterwebz;

import ai.arcblroth.projectInception.postlaunch.PostLaunchEntrypoint;
import ai.arcblroth.projectInception.postlaunch.ProgressBar;

public class CEFDownloader implements PostLaunchEntrypoint {

    @Override
    public void onPostLaunch(ProgressBar bar) {
        bar.setText("some long running operation [0/1000]");
        for(int i = 0; i < 1000; i++) {
            bar.setProgress(i / 1000F);
            bar.setText(String.format("some long running operation [%d/1000]", i));
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {}
        }
    }

}
