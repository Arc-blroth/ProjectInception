package ai.arcblroth.projectInception.client.taterwebz;

import ai.arcblroth.projectInception.ProjectInceptionClient;
import ai.arcblroth.projectInception.ProjectInceptionEarlyRiser;
import ai.arcblroth.projectInception.block.GameMultiblock;
import ai.arcblroth.projectInception.block.TaterwebzBlockEntity;
import ai.arcblroth.projectInception.client.AbstractGameInstance;
import ai.arcblroth.projectInception.client.mc.QueueProtocol;
import net.openhft.chronicle.queue.ExcerptAppender;

public class TaterwebzInstance extends AbstractGameInstance<TaterwebzBlockEntity> {

    public TaterwebzInstance(GameMultiblock<TaterwebzBlockEntity> multiblock) {
        super(multiblock);
    }

    @Override
    protected String getNewInstanceQueueDirectory() {
        return ProjectInceptionEarlyRiser.BROWSER_PREFIX + "-" + instanceNumber;
    }

    @Override
    public void start() {
        ExcerptAppender appender = ProjectInceptionClient.TATERWEBZ_CHILD_QUEUE.acquireAppender();
        QueueProtocol.RequestBrowserMessage rbMessage = new QueueProtocol.RequestBrowserMessage();
        rbMessage.createOrDestroy = true;
        rbMessage.uuid = instanceNumber;
        rbMessage.width = multiblock.sizeX * ProjectInceptionEarlyRiser.DISPLAY_SCALE;
        rbMessage.height = multiblock.sizeY * ProjectInceptionEarlyRiser.DISPLAY_SCALE;
        QueueProtocol.writeParent2ChildMessage(rbMessage, appender);
        super.start();
    }

    @Override
    protected void stopInner() {
        ExcerptAppender appender = ProjectInceptionClient.TATERWEBZ_CHILD_QUEUE.acquireAppender();
        QueueProtocol.RequestBrowserMessage rbMessage = new QueueProtocol.RequestBrowserMessage();
        rbMessage.createOrDestroy = false;
        rbMessage.uuid = instanceNumber;
        QueueProtocol.writeParent2ChildMessage(rbMessage, appender);
    }

    @Override
    public boolean isAlive() {
        return ProjectInceptionClient.TATERWEBZ_CHILD_PROCESS.isAlive();
    }

}