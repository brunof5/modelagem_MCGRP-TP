package tcc.br;

/**
 * Representa um arco direcionado.
 */
public class Arc {
    int fromNode;
    int toNode;
    int traversalCost;      // t_ij^d: tempo de deadheading
    int demand;
    int serviceCost;        // t_ij^s: tempo de serviço
    boolean isRequired;
    
    public Arc(int fromNode, int toNode, int traversalCost, int demand, int serviceCost, boolean isRequired) {
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.traversalCost = traversalCost;
        this.demand = isRequired ? 1 : 0;       // força a demanda a ser unitária
        this.serviceCost = serviceCost;
        this.isRequired = isRequired;
    }
}
