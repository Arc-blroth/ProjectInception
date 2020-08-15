package ai.arcblroth.projectInception.duck;

import ai.arcblroth.projectInception.QueueProtocol;

import java.util.List;

public interface IPreventMouseFromStackOverflow {

    public void projectInceptionUpdateMouseEvents(List<QueueProtocol.Message> events);
    public boolean getShouldPreventStackOverflow();

}
