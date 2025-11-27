package tcc.br;

import java.util.List;
import java.util.Map;

/**
 * Encapsula todas as estruturas de dados pré-processadas, prontas para serem consumidas pelo modelo CPLEX.
 * <p>- numVehicles (K): Conjunto de veículos idênticos da frota.
 * <p>- capacity (Tmax): Limite de tempo (duração) da rota.
 * <p>- realDepotNodeId: ID do nó em V' que representa o depósito.
 * <p>- reqNodes (R_V): Vértices de serviço requeridos.
 * <p>- reqEdges (E_R): Arestas de serviço requeridas.
 * <p>- reqArcsOg (A'_R): Arcos de serviço requeridos originais.
 * <p>- nodes (V'): Conjunto total de nós da instância (1...N).
 * <p>- arcs (A): Grafo de formulação totalmente direcionado.
 * <p>- reqArcs (R_A): Conjunto total de arcos de serviço requeridos (de A'_R e E_R).
 * <p>- turns (Turns): Conjunto de penalidades de conversão.
 */
public class CplexData {
    private final int numVehicles;              // K
    private final int capacity;                 // Tmax
    private final int realDepotNodeId;

    private final Map<Integer, Node> nodes;     // V'
    private final Map<String, Arc> arcs;        // A: chave String "i-j"
    private final Map<String, Arc> reqArcs;     // R_A: chave String "i-j"
    private final Map<String, Turn> turns;      // Turns

    private final Map<Integer, Node> reqNodes;  // R_V
    private final Map<Integer, Edge> reqEdges;  // E_R
    private final Map<Integer, Arc> reqArcsOg;  // A'_R

    private final Map<Integer, List<Arc>> outgoingArcsFrom;     // Arcos que SAEM do nó i
    private final Map<Integer, List<Arc>> incomingArcsTo;       // Arcos que CHEGAM no nó i

    public CplexData(int numVehicles, int capacity, int realDepotNodeId,
                     Map<Integer, Arc> reqArcsOg, Map<Integer, Edge> reqEdges, Map<Integer, Node> reqNodes,
                     Map<Integer, Node> nodes, Map<String, Arc> arcs, Map<String, Arc> reqArcs,
                     Map<Integer, List<Arc>> outgoingArcsFrom, Map<Integer, List<Arc>> incomingArcsTo,
                     Map<String, Turn> turns) {
        this.numVehicles = numVehicles;
        this.capacity = capacity;
        this.realDepotNodeId = realDepotNodeId;
        this.reqArcsOg = reqArcsOg;
        this.reqEdges = reqEdges;
        this.reqNodes = reqNodes;
        this.nodes = nodes;
        this.arcs = arcs;
        this.reqArcs = reqArcs;
        this.outgoingArcsFrom = outgoingArcsFrom;
        this.incomingArcsTo = incomingArcsTo;
        this.turns = turns;
    }

    public int getNumVehicles() {
        return numVehicles;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getRealDepotNodeId() {
        return realDepotNodeId;
    }
    
    public Map<Integer, Node> getNodes() {
        return nodes;
    }

    public Map<String, Arc> getArcs() {
        return arcs;
    }

    public Map<String, Arc> getReqArcs() {
        return reqArcs;
    }

    public Map<Integer, List<Arc>> getOutgoingArcsFrom() {
        return outgoingArcsFrom;
    }

    public Map<Integer, List<Arc>> getIncomingArcsTo() {
        return incomingArcsTo;
    }

    public Map<String, Turn> getTurns() {
        return turns;
    }

    public Map<Integer, Node> getReqNodes() {
        return reqNodes;
    }

    public Map<Integer, Edge> getReqEdges() {
        return reqEdges;
    }

    public Map<Integer, Arc> getReqArcsOg() {
        return reqArcsOg;
    }
}
