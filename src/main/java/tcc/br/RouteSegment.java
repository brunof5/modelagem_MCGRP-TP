package tcc.br;

/**
 * Representa um segmento da rota, conforme a notação (D, T, S).
 */
public class RouteSegment {
    String type;                // "D", "T", ou "S"
    int serviceId;
    int fromNode;
    int toNode;

    // 'T': Travessia
    public RouteSegment(String type, int fromNode, int toNode) {
        this.type = type;
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.serviceId = 0;
    }

    // 'D': Depósito ou 'S': Serviço
    public RouteSegment(String type, int serviceId, int fromNode, int toNode) {
        this.type = type;
        this.serviceId = serviceId;
        this.fromNode = fromNode;
        this.toNode = toNode;
    }

    @Override
    public String toString() {
        if ("T".equals(type)) {
            return String.format("(T %d,%d)", fromNode, toNode);
        }
        return String.format("(%s %d,%d,%d)", type, serviceId, fromNode, toNode);
    }
    
    public String getType() {
        return type;
    }

    public int getFromNode() {
        return fromNode;
    }
    
    public int getToNode() {
        return toNode;
    }
}
