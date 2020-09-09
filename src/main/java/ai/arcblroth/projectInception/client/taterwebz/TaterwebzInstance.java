package ai.arcblroth.projectInception.client.taterwebz;

import ai.arcblroth.projectInception.ProjectInceptionClient;
import ai.arcblroth.projectInception.ProjectInceptionEarlyRiser;
import ai.arcblroth.projectInception.block.GameMultiblock;
import ai.arcblroth.projectInception.block.TaterwebzBlockEntity;
import ai.arcblroth.projectInception.client.AbstractGameInstance;
import ai.arcblroth.projectInception.client.mc.QueueProtocol;
import ai.arcblroth.projectInception.config.ProjectInceptionConfig;
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
        rbMessage.width = multiblock.sizeX * ProjectInceptionConfig.TATERWEBZ_SCALE;
        rbMessage.height = multiblock.sizeY * ProjectInceptionConfig.TATERWEBZ_SCALE;
        QueueProtocol.writeParent2ChildMessage(rbMessage, appender);
        QueueProtocol.SetPageMessage spMessage = new QueueProtocol.SetPageMessage();
        spMessage.action = QueueProtocol.SetPageMessage.ACTION_GOTO;
        spMessage.url = ProjectInceptionConfig.TATERWEBZ_HOME_PAGE;
        QueueProtocol.writeParent2ChildMessage(spMessage, this.childQueue.acquireAppender());
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