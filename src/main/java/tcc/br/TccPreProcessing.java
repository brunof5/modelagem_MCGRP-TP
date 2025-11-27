package tcc.br;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Classe responsável por pré-processar os dados lidos da instância
 * e transformá-los nas estruturas necessárias para a formulação.
 */
public class TccPreProcessing {
    private CplexData cplexData;

    private final Map<String, Arc> arcs = new HashMap<>();
    private final Map<String, Arc> reqArcs = new HashMap<>();
    private final Map<Integer, List<Arc>> outgoingArcsFrom = new HashMap<>();
    private final Map<Integer, List<Arc>> incomingArcsTo = new HashMap<>();

    /**
     * Construtor que recebe os dados brutos e inicia o pré-processamento.
     */
    public TccPreProcessing(int numVehicles, int capacity, int realDepotNodeId,
                            Map<Integer, Node> reqNodesIn, Map<Integer, Node> nonReqNodesIn,
                            Map<Integer, Edge> reqEdgesIn, Map<Integer, Edge> nonReqEdgesIn,
                            Map<Integer, Arc> reqArcsIn, Map<Integer, Arc> nonReqArcsIn,
                            Map<String, Turn> turnsIn) {
        
        processData(numVehicles, capacity, realDepotNodeId, 
                    reqNodesIn, nonReqNodesIn, reqEdgesIn, nonReqEdgesIn, 
                    reqArcsIn, nonReqArcsIn, turnsIn);
    }

    /**
     * Executa a lógica de transformação de dados.
     */
    private void processData(int numVehicles, int capacity, int realDepotNodeId,
                             Map<Integer, Node> reqNodesIn, Map<Integer, Node> nonReqNodesIn,
                             Map<Integer, Edge> reqEdgesIn, Map<Integer, Edge> nonReqEdgesIn,
                             Map<Integer, Arc> reqArcsIn, Map<Integer, Arc> nonReqArcsIn,
                             Map<String, Turn> turnsIn) {

        Map<Integer, Arc> reqArcsOg = new HashMap<>(reqArcsIn);

        Map<Integer, Edge> reqEdges = new HashMap<>(reqEdgesIn);

        Map<Integer, Node> reqNodes = new HashMap<>(reqNodesIn);
        
        Map<String, Turn> turns = new HashMap<>(turnsIn);

        Map<Integer, Node> nodes = new HashMap<>(reqNodesIn);
        nodes.putAll(nonReqNodesIn);

        processArcMap(reqArcsIn);
        processArcMap(nonReqArcsIn);
        
        processEdgeMap(reqEdgesIn);
        processEdgeMap(nonReqEdgesIn);

        numVehicles = calculateTopNumVehicles(numVehicles, capacity, reqNodes);

        this.cplexData = new CplexData(numVehicles, capacity, realDepotNodeId, reqArcsOg, reqEdges, reqNodes, nodes, this.arcs, this.reqArcs, this.outgoingArcsFrom, this.incomingArcsTo, turns);
    }

    /**
     * Adiciona um arco às 4 estruturas principais:
     * arcs, reqArcs (se aplicável), outgoingArcsFrom, incomingArcsTo.
     */
    private void addArcToStructures(Arc arc) {
        String key = arc.fromNode + "-" + arc.toNode;
        
        this.arcs.put(key, arc);
        
        if (arc.isRequired) {
            this.reqArcs.put(key, arc);
        }

        this.outgoingArcsFrom.computeIfAbsent(arc.fromNode, k -> new ArrayList<>()).add(arc);
        
        this.incomingArcsTo.computeIfAbsent(arc.toNode, k -> new ArrayList<>()).add(arc);
    }

    /**
     * Adiciona arcos do mapa de arcos original (A') aos novos mapas 'arcs' (A) e 'reqArcs' (R_A).
     */
    private void processArcMap(Map<Integer, Arc> arcMap) {
        for (Arc a : arcMap.values()) {
            addArcToStructures(a);
        }
    }

    /**
     * Transforma arestas (E) em pares de arcos opostos e os adiciona
     * aos novos mapas 'arcs' (A) e 'reqArcs' (R_A).
     */
    private void processEdgeMap(Map<Integer, Edge> edgeMap) {
        for (Edge e : edgeMap.values()) {
            // Cria arco (i, j)
            Arc arc1 = new Arc(e.fromNode, e.toNode, e.traversalCost, e.demand, e.serviceCost, e.isRequired);
            addArcToStructures(arc1);

            // Cria arco (j, i)
            Arc arc2 = new Arc(e.toNode, e.fromNode, e.traversalCost, e.demand, e.serviceCost, e.isRequired);
            addArcToStructures(arc2);
        }
    }

    /**
     * Estabelece um limite superior de veículos esperados.
     */
    private int calculateTopNumVehicles(int numVehicles, int capacity, Map<Integer, Node> reqNodes) {
        int serviceCost = 0;

        for (Node n : reqNodes.values()) {
            serviceCost += n.serviceCost;
        }

        for (Arc a : reqArcs.values()) {
            serviceCost += a.serviceCost;
        }

        System.out.println("Valor total de serviço: " + serviceCost);

        if (numVehicles == -1) {
            return numVehicles = serviceCost / capacity;
        }
        return numVehicles;
    }

    /**
     * Retorna o pacote de dados processados.
     */
    public CplexData getCplexData() {
        return this.cplexData;
    }
}
