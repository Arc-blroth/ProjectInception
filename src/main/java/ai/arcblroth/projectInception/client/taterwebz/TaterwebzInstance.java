package ai.arcblroth.projectInception.client.taterwebz;

import ai.arcblroth.projectInception.ProjectInceptionClient;
import ai.arcblroth.projectInception.ProjectInceptionEarlyRiser;
import ai.arcblroth.projectInception.block.GameMultiblock;
import ai.arcblroth.projectInception.block.TaterwebzBlockEntity;
import ai.arcblroth.projectInception.client.AbstractGameInstance;
import ai.arcblroth.projectInception.client.mc.QueueProtocol;
import ai.arcblroth.projectInception.config.ProjectInceptionConfig;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.TailerDirection;
import net.openhft.chronicle.wire.DocumentContext;

import static ai.arcblroth.projectInception.client.mc.QueueProtocol.readParent2ChildMessage;

public class TaterwebzInstance extends AbstractGameInstance<TaterwebzBlockEntity> {

    private final ExcerptTailer secondTailer;
    private String lastURL = "about:blank";

    public TaterwebzInstance(GameMultiblock<TaterwebzBlockEntity> multiblock) {
        super(multiblock);
        this.secondTailer = this.childQueue.createTailer("URL Change Tailer").direction(TailerDirection.FORWARD);
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

    @Override
    protected void tailerLoopInner() {
        while (true) {
            try(DocumentContext dc = secondTailer.readingDocument()) {
                if(dc.isPresent()) {
                    Bytes<?> bytes = dc.wire().bytes();
                    // SET_PAGE messages are implemented in parent2child, but
                    // are sent from both sides.
                    QueueProtocol.Message message = readParent2ChildMessage(bytes);
                    QueueProtocol.MessageType type = message.getMessageType();
                    if(type == QueueProtocol.MessageType.SET_PAGE) {
                        QueueProtocol.SetPageMessage spMessage = (QueueProtocol.SetPageMessage) message;
                        if(spMessage.action == QueueProtocol.SetPageMessage.ACTION_GOTO) {
                            lastURL = spMessage.url;
                        }
                    }
                } else {
                    dc.rollbackOnClose();
                    break;
                }
            }
        }
    }

    public void gotoUrl(String url) {
        QueueProtocol.SetPageMessage spMessage = new QueueProtocol.SetPageMessage();
        spMessage.action = QueueProtocol.SetPageMessage.ACTION_GOTO;
        spMessage.url = url;
        QueueProtocol.writeParent2ChildMessage(spMessage, this.childQueue.acquireAppender());
    }

    public void goBack() {
        QueueProtocol.SetPageMessage spMessage = new QueueProtocol.SetPageMessage();
        spMessage.action = QueueProtocol.SetPageMessage.ACTION_BACK;
        QueueProtocol.writeParent2ChildMessage(spMessage, this.childQueue.acquireAppender());
    }

    public void goForward() {
        QueueProtocol.SetPageMessage spMessage = new QueueProtocol.SetPageMessage();
        spMessage.action = QueueProtocol.SetPageMessage.ACTION_FORWARD;
        QueueProtocol.writeParent2ChildMessage(spMessage, this.childQueue.acquireAppender());
    }

    public void reload() {
        QueueProtocol.SetPageMessage spMessage = new QueueProtocol.SetPageMessage();
        spMessage.action = QueueProtocol.SetPageMessage.ACTION_RELOAD;
        QueueProtocol.writeParent2ChildMessage(spMessage, this.childQueue.acquireAppender());
    }

    public String getCurrentURL() {
        return lastURL;
    }

}