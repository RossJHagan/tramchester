package com.tramchester.graph.Relationships;

import org.neo4j.graphdb.Relationship;

public class BoardRelationship extends TramCostRelationship {

    public BoardRelationship(int cost) {
        super(cost);
    }

    public BoardRelationship(Relationship graphRelationship) {
        super(graphRelationship);
    }

    @Override
    public boolean isGoesTo() {
        return false;
    }

    @Override
    public boolean isBoarding() {
        return true;
    }

    @Override
    public boolean isDepartTram() {
        return false;
    }

    @Override
    public boolean isInterchange() {
        return false;
    }

    @Override
    public String toString() {
        return "BoardRelationship{cost:"+ super.getCost() +"}";
    }
}