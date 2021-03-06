package com.tramchester.graph.Relationships;

import com.tramchester.domain.TransportMode;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import org.neo4j.graphdb.Relationship;

public class InterchangeDepartsRelationship extends TransportCostRelationship {
    public InterchangeDepartsRelationship(Relationship graphRelationship, NodeFactory nodeFactory) {
        super(graphRelationship, nodeFactory);
    }

    public static InterchangeDepartsRelationship TestOnly(int cost, String id, TramNode firstNode, TramNode endNode) {
        return new InterchangeDepartsRelationship(cost,id, firstNode, endNode);
    }

    private InterchangeDepartsRelationship(int cost, String id, TramNode startNode, TramNode endNode) {
        super(cost,id,startNode, endNode);
    }

    @Override
    public TransportMode getMode() {
        return TransportMode.Depart;
    }

    @Override
    public boolean isDepartTram() {
        return true;
    }

    @Override
    public boolean isInterchange() { return true; }

    @Override
    public String toString() {
        return "InterchangeDepartsRelationship{cost:"+ super.getCost() +", id:" + super.getId() + "}";
    }
}
