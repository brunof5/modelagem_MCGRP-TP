package tcc.br;

import java.util.ArrayList;
import java.util.List;

/**
 * Armazena a solução completa do problema, incluindo rotas, custos e
 * estatísticas de execução.
 */
public class Solution {
    private int objectiveValue;
    private int numVehiclesUsed;
    private long totalExecutionTimeMillis;
    private long timeToBestSolutionMillis;
    private List<Route> routes;
    
    public Solution() {
        this.routes = new ArrayList<>();
    }

    public int getObjectiveValue() {
        return objectiveValue;
    }

    public void setObjectiveValue(int objectiveValue) {
        this.objectiveValue = objectiveValue;
    }

    public int getNumVehiclesUsed() {
        return numVehiclesUsed;
    }

    public void setNumVehiclesUsed(int numVehiclesUsed) {
        this.numVehiclesUsed = numVehiclesUsed;
    }

    public long getTotalExecutionTimeMillis() {
        return totalExecutionTimeMillis;
    }

    public void setTotalExecutionTimeMillis(long totalExecutionTimeMillis) {
        this.totalExecutionTimeMillis = totalExecutionTimeMillis;
    }

    public long getTimeToBestSolutionMillis() {
        return timeToBestSolutionMillis;
    }

    public void setTimeToBestSolutionMillis(long timeToBestSolutionMillis) {
        this.timeToBestSolutionMillis = timeToBestSolutionMillis;
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public void setRoutes(List<Route> routes) {
        this.routes = routes;
    }

    public void addRoute(Route route) {
        this.routes.add(route);
    }
}
