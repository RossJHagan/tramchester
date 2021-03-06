package com.tramchester.graph.Relationships;

import com.tramchester.domain.TransportMode;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.Nodes.TramNode;

public interface TransportRelationship {
    boolean isGoesTo();
    boolean isBoarding();
    boolean isDepartTram();
    boolean isInterchange();
    boolean isWalk();
    boolean isEnterPlatform();
    boolean isLeavePlatform();

    int getCost();
    String getId();
    TransportMode getMode();
    TramNode getStartNode() throws TramchesterException;
    TramNode getEndNode() throws TramchesterException;

}
