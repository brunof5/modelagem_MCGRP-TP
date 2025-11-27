package tcc.br;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa uma rota de um único veículo.
 */
public class Route {
    private int totalDemand;
    private int routeCost;                   // Z_k
    private List<RouteSegment> segments;

    public Route() {
        this.segments = new ArrayList<>();
    }

    public void addSegment(RouteSegment segment) {
        this.segments.add(segment);
    }

    public List<RouteSegment> getSegments() { 
        return segments;
    }

    public int getTotalDemand() {
        return totalDemand;
    }

    public void setTotalDemand(int totalDemand) {
        this.totalDemand = totalDemand;
    }

    public int getRouteCost() {
        return routeCost;
    }

    public void setRouteCost(int routeCost) {
        this.routeCost = routeCost;
    }
}
