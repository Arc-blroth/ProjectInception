package ai.arcblroth.taterwebz.util;

import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.client.mc.QueueProtocol;
import ai.arcblroth.projectInception.postlaunch.ProgressBar;

public class HeadlessProgressBar extends ProgressBar {

    private final QueueProtocol.LoadProgressMessage lpMessage;

    public HeadlessProgressBar() {
        lpMessage = new QueueProtocol.LoadProgressMessage();
        lpMessage.progress = getProgress();
        lpMessage.text = getText();
    }

    @Override
    public synchronized void setProgress(float progress) {
        super.setProgress(progress);
        lpMessage.progress = progress;
    }

    @Override
    public synchronized void setText(String text) {
        super.setText(text);
        lpMessage.text = text;
    }

    public synchronized void setDone(boolean done) {
        lpMessage.done = done;
    }

    public synchronized void update() {
        QueueProtocol.writeChild2ParentMessage(lpMessage, ProjectInception.toParentQueue.acquireAppender());
    }

}
