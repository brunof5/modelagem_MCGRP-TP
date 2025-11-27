package tcc.br;

/**
 * Representa um nó da instância.
 */
public class Node {
    int demand;
    int serviceCost;        // t_i^s: tempo de serviço no nó
    boolean isRequired;

    public Node() {
        this.demand = 0;
        this.serviceCost = 0;
        this.isRequired = false;
    }

    public Node(int demand, int serviceCost, boolean isRequired) {
        this.demand = isRequired ? 1 : 0;       // força a demanda a ser unitária
        this.serviceCost = serviceCost;
        this.isRequired = isRequired;
    }
}
