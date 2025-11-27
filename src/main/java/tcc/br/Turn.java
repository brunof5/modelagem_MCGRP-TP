package tcc.br;

public class Turn {
    int i;          // fromNode
    int j;          // midNode
    int l;          // toNode
    int cost;       // c_ijl: penalidade de tempo
    String type;

    public Turn(int i, int j, int l, int cost, String type) {
        this.i = i;
        this.j = j;
        this.l = l;
        this.cost = cost;
        this.type = type;
    }
}
