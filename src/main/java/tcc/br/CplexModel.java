package tcc.br;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloLinearNumExprIterator;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;

import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.MIPInfoCallback;

/**
 * Classe principal para a construção e resolução do modelo CPLEX.
 * Lida com as formulações NEARP e NEARP-TP.
 */
public class CplexModel {
    // --- Tempo de Execução ---
    private final long MAX_RUNTIME = 3_600_000;    // milliseconds; 1 hour

    // --- Dados e Controle ---
    private final CplexData data;
    private final boolean isTurnPenaltiesModel;
    private final int K;                            // Número de veículos
    
    // --- Solver ---
    private IloCplex cplex;

    // --- Variáveis de Decisão ---
    // x_ij^k (Binária): Veículo k serve o arco (i,j) em R_A
    private Map<String, IloNumVar> x;
    
    // z_i^k (Binária): Veículo k serve o nó i em R_V
    private Map<String, IloNumVar> z;
    
    // y_ij^k (Inteira): Veículo k atravessa o arco (i,j) em A (deadheading)
    private Map<String, IloNumVar> y;
    
    // f_ij^k (Contínua): Fluxo de tempo no arco (i,j) para o veículo k
    private Map<String, IloNumVar> f;

    // w_ijl^k (Inteira): Veículo k faz a conversão (i,j,l) em Turns
    private Map<String, IloNumVar> w;

    // Expressão Z_k para cada veículo k
    private Map<Integer, IloLinearNumExpr> z_k_expressions;

    private double bestObjectiveFound;
    private long timeToBestSolutionMillis;

    private Map<String, Integer> serviceId;

    /**
     * Construtor do CplexModel.
     * @param data O pacote de dados pré-processados.
     * @param inputType O tipo de problema ("NEARP" ou "NEARPTP")
     */
    public CplexModel(CplexData data, String inputType) {
        this.data = data;
        this.isTurnPenaltiesModel = inputType.equalsIgnoreCase("NEARPTP");
        this.K = data.getNumVehicles();

        try {
            this.cplex = new IloCplex();
        } catch (IloException e) {
            System.err.println("Erro ao instanciar o IloCplex");
            e.printStackTrace();
        }

        // Inicializa os mapas
        this.x = new HashMap<>();
        this.z = new HashMap<>();
        this.y = new HashMap<>();
        this.f = new HashMap<>();
        this.w = new HashMap<>();
        this.z_k_expressions = new HashMap<>();
    }

    /**
     * Método principal para construir e resolver o modelo.
     * @param stopWatch O cronômetro global da aplicação.
     */
    public Solution solve(StopWatch stopWatch) {
        System.out.println("Construindo o modelo CPLEX...");
        try {
            // 1. Criar Variáveis de Decisão
            buildDecisionVariables();

            // 2. Criar Função Objetivo
            buildObjectiveFunction();

            // 3. Criar Restrições
            buildConstraints();

            System.out.println("\nModelo CPLEX construído com sucesso.");
            
            // Parâmetros do CPLEX
            cplex.setParam(IloCplex.Param.TimeLimit, getRemainingTime(stopWatch));
            //cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, 0.01);
            cplex.setParam(IloCplex.Param.MIP.Strategy.File, 3);

            String lpFilename = "debug_model.lp";
            cplex.exportModel(lpFilename);
            System.out.println(">>> DEBUG: Modelo exportado para " + lpFilename);

            attachMIPInfoCallback(stopWatch);

            boolean solved = cplex.solve();
            Solution sol = null;

            if (solved) {
                IloCplex.Status status = cplex.getStatus();
                System.out.println("Solução encontrada! Estado: " + status);

                saveDecisionVariables("solution_variables.txt");

                long totalTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                sol = buildSolution(totalTime);
            } else {
                IloCplex.Status status = cplex.getStatus();
                System.err.println("Solução não encontrada!!! Estado: " + status);
            }

            cplex.end();
            return sol;

        } catch (IloException e) {
            System.err.println("Um erro de CPLEX ocorreu ao construir o modelo:");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Um erro inesperado ocorreu:");
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Cria todas as variáveis de decisão da formulação.
     */
    private void buildDecisionVariables() throws IloException {
        System.out.println("  \nCriando variáveis...");

        // Variáveis x_ij^k (Binária) - Para arcos em R_A
        System.out.println("    ... x (serviço de arco)");
        for (int k = 0; k < K; k++) {
            for (Arc arc : data.getReqArcs().values()) {        // Iterar sobre R_A
                String key = k + "-" + arc.fromNode + "-" + arc.toNode;
                String name = "x(" + k + "," + arc.fromNode + "," + arc.toNode + ")";
                IloNumVar var = cplex.boolVar(name);
                x.put(key, var);
            }
        }

        System.out.println("    Total de variáveis x: " + x.size());

        // Variáveis z_i^k (Binária) - Para nós em R_V
        System.out.println("    ... z (serviço de nó)");
        for (int k = 0; k < K; k++) {
            for (Integer nodeId : data.getReqNodes().keySet()) {        // Iterar sobre R_V
                String key = k + "-" + nodeId;
                String name = "z(" + k + "," + nodeId + ")";
                IloNumVar var = cplex.boolVar(name);
                z.put(key, var);
            }
        }

        System.out.println("    Total de variáveis z: " + z.size());

        // Variáveis y_ij^k (Inteira) - Para arcos em A
        System.out.println("    ... y (deadheading)");
        for (int k = 0; k < K; k++) {
            for (Arc arc : data.getArcs().values()) {       // Iterar sobre A
                String key = k + "-" + arc.fromNode + "-" + arc.toNode;
                String name = "y(" + k + "," + arc.fromNode + "," + arc.toNode + ")";
                IloNumVar var = cplex.intVar(0, Integer.MAX_VALUE, name);
                y.put(key, var);
            }
        }

        System.out.println("    Total de variáveis y: " + y.size());

        // Variáveis f_ij^k (Contínua) - Para arcos em A
        System.out.println("    ... f (fluxo de tempo)");
        for (int k = 0; k < K; k++) {
            for (Arc arc : data.getArcs().values()) {       // Iterar sobre A
                String key = k + "-" + arc.fromNode + "-" + arc.toNode;
                String name = "f(" + k + "," + arc.fromNode + "," + arc.toNode + ")";
                IloNumVar var = cplex.numVar(0, Double.MAX_VALUE, name);
                f.put(key, var);
            }
        }

        System.out.println("    Total de variáveis f: " + f.size());

        // Variáveis w_ijl^k (Inteira) - Apenas para NEARP-TP
        if (isTurnPenaltiesModel) {
            System.out.println("    ... w (fluxo de conversão)");
            for (int k = 0; k < K; k++) {
                for (Turn turn : data.getTurns().values()) {        // Iterar sobre Turns
                    String key = k + "-" + turn.i + "-" + turn.j + "-" + turn.l;
                    String name = "w(" + k + "," + turn.i + "," + turn.j + "," + turn.l + ")";
                    IloNumVar var = cplex.intVar(0, Integer.MAX_VALUE, name);
                    w.put(key, var);
                }
            }
            System.out.println("    Total de variáveis w: " + w.size());
        }

        System.out.println("  Variáveis criadas.");
    }

    private void buildObjectiveFunction() throws IloException {
        System.out.println("  \nConstruindo Função Objetivo...");
        IloLinearNumExpr objective = cplex.linearNumExpr();

        for (int k = 0; k < K; k++) {
            IloLinearNumExpr zkExpr = cplex.linearNumExpr();

            // Custo de serviço em arcos (t_ij^s * x_ij^k)
            for (Arc arc : data.getReqArcs().values()) {        // (i,j) in R_A
                String x_key = k + "-" + arc.fromNode + "-" + arc.toNode;
                zkExpr.addTerm(arc.serviceCost, x.get(x_key));
            }

            // Custo de serviço em nós (t_i^s * z_i^k)
            for (Map.Entry<Integer, Node> entry : data.getReqNodes().entrySet()) {      // i in R_V
                int nodeId = entry.getKey();
                Node node = entry.getValue();
                String z_key = k + "-" + nodeId;
                zkExpr.addTerm(node.serviceCost, z.get(z_key));
            }

            // Custo de deadheading (t_ij^d * y_ij^k)
            for (Arc arc : data.getArcs().values()) {       // (i,j) in A
                String y_key = k + "-" + arc.fromNode + "-" + arc.toNode;
                zkExpr.addTerm(arc.traversalCost, y.get(y_key));
            }

            // Custo de conversão (c_ijl * w_ijl^k) - Apenas para NEARP-TP
            if (isTurnPenaltiesModel) {
                for (Turn turn : data.getTurns().values()) {        // (i,j,l) in Turns
                    String w_key = k + "-" + turn.i + "-" + turn.j + "-" + turn.l;
                    zkExpr.addTerm(turn.cost, w.get(w_key));
                }
            }

            this.z_k_expressions.put(k, zkExpr);
            objective.add(zkExpr);
        }

        int count = 0;
        for (IloLinearNumExprIterator it = objective.linearIterator(); it.hasNext(); it.next()) {
            count++;
        }
        System.out.println("    Número de termos: " + count);

        cplex.addMinimize(objective);
        System.out.println("  Função Objetivo construída.");
    }
    
    /**
     * Método principal para construir todas as restrições.
     */
    private void buildConstraints() throws IloException {
        System.out.println("  \nConstruindo Restrições...");

        // Restrições de Atribuição de Tarefas
        int r1 = buildAssignmentConstraints();

        // Restrições de Fluxo de Veículos
        int r2 = buildVehicleFlowConstraints();

        // Restrições de Fluxo de Tempo
        int r3 = buildTimeFlowConstraints();

        // Restrições de Limitentes Inferiores para os Fluxos
        int r4 = buildFlowLowerBoundConstraints();
        
        int r5 = 0;
        // Restrições de Conversão
        if (isTurnPenaltiesModel) {
            r5 = buildTurnConstraints();
        }

        // Restrições de Quebra de Simetria
        int r6 = buildSymmetryBreakConstraints();

        System.out.println("  Restrições construídas.");
        System.out.println("  Quantidade de restrições: " + (r1 + r2 + r3 + r4 + r5 + r6));
    }

    /**
     * Constrói as restrições de Atribuição.
     */
    private int buildAssignmentConstraints() throws IloException {
        // (1) sum_k x_ij^k = 1, for all (i,j) in A'_R
        System.out.print("    ... (1) Atribuição de Arcos");
        int cont1 = 0;
        for (Arc arc : data.getReqArcsOg().values()) {
            IloLinearNumExpr sumX = cplex.linearNumExpr();
            for (int k = 0; k < K; k++) {
                String key = k + "-" + arc.fromNode + "-" + arc.toNode;
                sumX.addTerm(1.0, x.get(key));
            }
            cplex.addEq(sumX, 1.0, "Assign_Arc_" + arc.fromNode + "_" + arc.toNode);
            cont1++;
        }
        System.out.println("\t\t\tQtd: " + cont1);

        // (2) sum_k (x_ij^k + x_ji^k) = 1, for all (i,j) in E_R
        System.out.print("    ... (2) Atribuição de Arestas");
        int cont2 = 0;
        for (Edge edge : data.getReqEdges().values()) {
            IloLinearNumExpr sumX_Edge = cplex.linearNumExpr();
            for (int k = 0; k < K; k++) {
                // Arco (i,j)
                String key1 = k + "-" + edge.fromNode + "-" + edge.toNode;
                // Arco (j,i)
                String key2 = k + "-" + edge.toNode + "-" + edge.fromNode;
                
                sumX_Edge.addTerm(1.0, x.get(key1));
                sumX_Edge.addTerm(1.0, x.get(key2));
            }
            cplex.addEq(sumX_Edge, 1.0, "Assign_Edge_" + edge.fromNode + "_" + edge.toNode);
            cont2++;
        }
        System.out.println("\t\tQtd: " + cont2);

        // (3) sum_k z_i^k = 1, for all i in R_V
        System.out.print("    ... (3) Atribuição de Nós");
        int cont3 = 0;
        for (Integer nodeId : data.getReqNodes().keySet()) {
            IloLinearNumExpr sumZ = cplex.linearNumExpr();
            for (int k = 0; k < K; k++) {
                String key = k + "-" + nodeId;
                sumZ.addTerm(1.0, z.get(key));
            }
            cplex.addEq(sumZ, 1.0, "Assign_Node_" + nodeId);
            cont3++;
        }
        System.out.println("\t\t\tQtd: " + cont3);

        return (cont1 + cont2 + cont3);
    }

    /**
     * Constrói as restrições de Fluxo de Veículos.
     */
    private int buildVehicleFlowConstraints() throws IloException {
        // (4) (Sum IN) - (Sum OUT) = 0, for all i in V', k in K
        System.out.print("    ... (4) Conservação de Fluxo");
        int cont4 = 0;
        for (int k = 0; k < K; k++) {
            for (Integer nodeId : data.getNodes().keySet()) {
                if (nodeId.equals(data.getRealDepotNodeId())) {
                    continue;
                }

                IloLinearNumExpr flowBalance = cplex.linearNumExpr();

                // --- FLUXO DE ENTRADA ---
                List<Arc> incoming = data.getIncomingArcsTo().get(nodeId);
                if (incoming != null) {
                    for (Arc arc : incoming) {
                        // + y_ji^k
                        String y_key = k + "-" + arc.fromNode + "-" + arc.toNode;
                        flowBalance.addTerm(1.0, y.get(y_key));

                        // + x_ji^k (se (j,i) in R_A)
                        String x_key = k + "-" + arc.fromNode + "-" + arc.toNode;
                        if (x.containsKey(x_key)) {
                            flowBalance.addTerm(1.0, x.get(x_key));
                        }
                    }
                }

                // --- FLUXO de SAÍDA ---
                List<Arc> outgoing = data.getOutgoingArcsFrom().get(nodeId);
                if (outgoing != null) {
                    for (Arc arc : outgoing) {
                        // - y_ij^k
                        String y_key = k + "-" + arc.fromNode + "-" + arc.toNode;
                        flowBalance.addTerm(-1.0, y.get(y_key));

                        // - x_ij^k (se (i,j) in R_A)
                        String x_key = k + "-" + arc.fromNode + "-" + arc.toNode;
                        if (x.containsKey(x_key)) {
                            flowBalance.addTerm(-1.0, x.get(x_key));
                        }
                    }
                }

                // (Sum IN) - (Sum OUT) = 0
                cplex.addEq(flowBalance, 0.0, "Flow_Cons_" + k + "_" + nodeId);
                cont4++;
            }
        }
        System.out.println("\t\tQtd: " + cont4);
        
        // (5) sum_j y_0j^k + sum_j x_0j^k <= 1, for all k in K
        System.out.print("    ... (5) Saída do Depósito");
        int cont5 = 0;
        for (int k = 0; k < K; k++) {
            IloLinearNumExpr depotDeparture = cplex.linearNumExpr();
            
            // Obtém todos os arcos que saem do depósito 0
            List<Arc> outgoingFromDepot = data.getOutgoingArcsFrom().get(data.getRealDepotNodeId());
            
            if (outgoingFromDepot != null) {
                for (Arc arc : outgoingFromDepot) {
                    // + y_0j^k
                    String y_key = k + "-" + arc.fromNode + "-" + arc.toNode;
                    depotDeparture.addTerm(1.0, y.get(y_key));

                    // + x_0j^k (se (0,j) in R_A)
                    String x_key = k + "-" + arc.fromNode + "-" + arc.toNode;
                    if (x.containsKey(x_key)) {
                        depotDeparture.addTerm(1.0, x.get(x_key));
                    }
                }
            }
            
            cplex.addLe(depotDeparture, 1.0, "Depot_Leave_" + k);
            cont5++;
        }
        System.out.println("\t\t\tQtd: " + cont5);

        return (cont4 + cont5);
    }

    /**
     * Constrói as restrições de Fluxo de Tempo.
     */
    private int buildTimeFlowConstraints() throws IloException {
        System.out.print("    ... (6) Conservação de Fluxo de Tempo");
        int cont6 = 0;
        for (int k = 0; k < K; k++) {
            for (Integer nodeId : data.getNodes().keySet()) {
                if (nodeId.equals(data.getRealDepotNodeId())) {
                    continue;
                }
                
                // (6) (Sum f_in) - (Sum f_out) - (RHS) = 0
                IloLinearNumExpr timeFlowBalance = cplex.linearNumExpr();

                // --- (Sum f_in) ---
                List<Arc> incoming = data.getIncomingArcsTo().get(nodeId);
                if (incoming != null) {
                    for (Arc arc : incoming) {
                        String f_key = k + "-" + arc.fromNode + "-" + arc.toNode;
                        timeFlowBalance.addTerm(1.0, f.get(f_key));
                    }
                }
                
                // --- (Sum f_out) ---
                List<Arc> outgoing = data.getOutgoingArcsFrom().get(nodeId);
                if (outgoing != null) {
                    for (Arc arc : outgoing) {
                        String f_key = k + "-" + arc.fromNode + "-" + arc.toNode;
                        timeFlowBalance.addTerm(-1.0, f.get(f_key));
                    }
                }

                // --- (RHS) ---
                // - (sum t_ji^s * x_ji^k) e - (sum t_ji^d * y_ji^k)
                if (incoming != null) {
                    for (Arc arc : incoming) {
                        // Custo de Serviço (x)
                        String x_key = k + "-" + arc.fromNode + "-" + arc.toNode;
                        if (x.containsKey(x_key)) {     // Se arc in R_A
                            timeFlowBalance.addTerm(-1.0 * arc.serviceCost, x.get(x_key));
                        }
                        
                        // Custo de Deadheading (y)
                        String y_key = k + "-" + arc.fromNode + "-" + arc.toNode;
                        timeFlowBalance.addTerm(-1.0 * arc.traversalCost, y.get(y_key));
                    }
                }

                // - (t_i^s * z_i^k)
                String z_key = k + "-" + nodeId;
                if (z.containsKey(z_key)) {     // Se node in R_V
                    timeFlowBalance.addTerm(-1.0 * data.getReqNodes().get(nodeId).serviceCost, z.get(z_key));
                }

                // - (sum c_ijl * w_ijl^k) --- Apenas para NEARP-TP
                if (isTurnPenaltiesModel) {
                    // Itera por Turns e pega apenas as conversões (i,j,l) onde j = nodeId
                    for (Turn turn : data.getTurns().values()) {
                        if (turn.j == nodeId) {
                            String w_key = k + "-" + turn.i + "-" + turn.j + "-" + turn.l;
                            timeFlowBalance.addTerm(-1.0 * turn.cost, w.get(w_key));
                        }
                    }
                }
                
                // Adiciona a restrição (LHS - RHS) = 0
                cplex.addEq(timeFlowBalance, 0.0, "TimeFlow_" + k + "_" + nodeId);
                cont6++;
            }
        }
        System.out.println("\tQtd: " + cont6);

        System.out.print("    ... (7, 8) Fluxo de Tempo do Depósito");
        int cont7 = 0;
        int cont8 = 0;
        for (int k = 0; k < K; k++) {
            // (7) sum f_0j^k = Z_k
            IloLinearNumExpr f_out_depot = cplex.linearNumExpr();
            List<Arc> outgoingFromDepot = data.getOutgoingArcsFrom().get(data.getRealDepotNodeId());
            if (outgoingFromDepot != null) {
                for (Arc arc : outgoingFromDepot) {
                    String f_key = k + "-" + arc.fromNode + "-" + arc.toNode;
                    f_out_depot.addTerm(1.0, f.get(f_key));
                }
            }
            cplex.addEq(f_out_depot, z_k_expressions.get(k), "Depot_Time_Start_" + k);
            cont7++;
            
            // (8) sum f_i0^k = sum (t^s x_i0^k + t^d y_i0^k)
            IloLinearNumExpr f_in_depot = cplex.linearNumExpr();        // LHS
            IloLinearNumExpr cost_in_depot = cplex.linearNumExpr();     // RHS
            
            List<Arc> incomingToDepot = data.getIncomingArcsTo().get(data.getRealDepotNodeId());
            if (incomingToDepot != null) {
                for (Arc arc : incomingToDepot) {
                    // LHS
                    String f_key = k + "-" + arc.fromNode + "-" + arc.toNode;
                    f_in_depot.addTerm(1.0, f.get(f_key));
                    
                    // RHS
                    // Custo de Deadheading (y)
                    String y_key = k + "-" + arc.fromNode + "-" + arc.toNode;
                    cost_in_depot.addTerm(arc.traversalCost, y.get(y_key));

                    // Custo de Serviço (x)
                    String x_key = k + "-" + arc.fromNode + "-" + arc.toNode;
                    if (x.containsKey(x_key)) {     // Se arc in R_A
                        cost_in_depot.addTerm(arc.serviceCost, x.get(x_key));
                    }
                }
            }
            cplex.addEq(f_in_depot, cost_in_depot, "Depot_Time_End_" + k);
            cont8++;
        }
        System.out.println("\tQtd: " + (cont7 + cont8));

        // (9) f_ij^k <= T_max * (x_ij^k + y_ij^k)
        System.out.print("    ... (9) Limite de Tempo (T_max)");
        int cont9 = 0;
        double T_max = data.getCapacity();

        for (int k = 0; k < K; k++) {
            for (Arc arc : data.getArcs().values()) {
                String f_key = k + "-" + arc.fromNode + "-" + arc.toNode;
                IloNumVar f_var = f.get(f_key);

                // RHS: T_max * (x_ij^k + y_ij^k)
                IloLinearNumExpr rhs_expr = cplex.linearNumExpr();
                
                // Termo y_ij^k
                String y_key = k + "-" + arc.fromNode + "-" + arc.toNode;
                rhs_expr.addTerm(T_max, y.get(y_key));

                // Termo x_ij^k (se existir)
                String x_key = k + "-" + arc.fromNode + "-" + arc.toNode;
                if (x.containsKey(x_key)) {
                    rhs_expr.addTerm(T_max, x.get(x_key));
                }

                cplex.addLe(f_var, rhs_expr, "Time_Link_" + k + "_" + arc.fromNode + "_" + arc.toNode);
                cont9++;
            }
        }

        System.out.println("\t\tQtd: " + cont9);
        return (cont6 + cont7 + cont8 + cont9);
    }

    /**
     * Constrói as restrições de Limite Inferior de Fluxo de Tempo.
     */
    private int buildFlowLowerBoundConstraints() throws IloException {
        System.out.print("    ... (17, 18) Limites Inferiores de Fluxo de Tempo");
        int cont17 = 0;
        int cont18 = 0;

        for (int k = 0; k < K; k++) {
            for (Arc arc : data.getArcs().values()) {
                String f_key = k + "-" + arc.fromNode + "-" + arc.toNode;
                IloNumVar f_var = f.get(f_key);

                String x_key = k + "-" + arc.fromNode + "-" + arc.toNode;

                if (x.containsKey(x_key)) {
                    // --- Restrição (18) ---
                    // f_ij^k >= t_ij^s * x_ij^k
                    IloNumVar x_var = x.get(x_key);
                    
                    // RHS: t_ij^s * x_ij^k
                    IloNumExpr rhs18 = cplex.prod(x_var, arc.serviceCost);
                    
                    cplex.addGe(f_var, rhs18, "Flow_LB_Serv_" + k + "_" + arc.fromNode + "_" + arc.toNode);
                    cont18++;

                } else {
                    // --- Restrição (17) ---
                    // f_ij^k >= t_ij^d * y_ij^k
                    String y_key = k + "-" + arc.fromNode + "-" + arc.toNode;
                    IloNumVar y_var = y.get(y_key);
                    
                    // RHS: t_ij^d * y_ij^k
                    IloNumExpr rhs17 = cplex.prod(y_var, arc.traversalCost);
                    
                    cplex.addGe(f_var, rhs17, "Flow_LB_Dead_" + k + "_" + arc.fromNode + "_" + arc.toNode);
                    cont17++;
                }
            }
        }
        System.out.println("\tQtd: " + (cont17 + cont18) + " (S:" + cont18 + ", D:" + cont17 + ")");
        return (cont17 + cont18);
    }

    /**
     * Constrói as restrições de Ligação de Conversão.
     */
    private int buildTurnConstraints() throws IloException {
        System.out.print("    ... (14, 15) Ligação de Fluxo de Conversão");
        int cont14 = 0, cont15 = 0;
        for (int k = 0; k < K; k++) {
            for (Arc arc : data.getArcs().values()) {
                int i = arc.fromNode;
                int j = arc.toNode;

                // --- Restrição (14) ---
                // LHS: sum(l) w_ijl^k
                IloLinearNumExpr lhs14 = cplex.linearNumExpr();
                
                // (Iteração ineficiente O(T). Para otimizar, pré-processe T_urns
                // em TccPreProcessing para ter um Map<String, List<Turn>> (chave "i-j"))
                if (isTurnPenaltiesModel) {
                    for (Turn turn : data.getTurns().values()) {
                        if (turn.i == i && turn.j == j) {
                            String w_key = k + "-" + turn.i + "-" + turn.j + "-" + turn.l;
                            lhs14.addTerm(1.0, w.get(w_key));
                        }
                    }
                }
                
                // RHS: x_ij^k + y_ij^k
                IloLinearNumExpr rhs14 = cplex.linearNumExpr();
                String y_key14 = k + "-" + i + "-" + j;
                rhs14.addTerm(1.0, y.get(y_key14));
                
                String x_key14 = k + "-" + i + "-" + j;
                if (x.containsKey(x_key14)) {
                    rhs14.addTerm(1.0, x.get(x_key14));
                }
                
                cplex.addEq(lhs14, rhs14, "Turn_Link_Out_" + k + "_" + i + "_" + j);
                cont14++;

                // --- Restrição (15) ---
                // (arc = (j,l))
                int j_node = arc.fromNode; // (j)
                int l_node = arc.toNode; // (l)

                // LHS: sum(i) w_ijl^k
                IloLinearNumExpr lhs15 = cplex.linearNumExpr();
                
                // (Iteração ineficiente O(T). Para otimizar, pré-processe T_urns
                // em TccPreProcessing para ter um Map<String, List<Turn>> (chave "j-l"))
                if (isTurnPenaltiesModel) {
                    for (Turn turn : data.getTurns().values()) {
                        if (turn.j == j_node && turn.l == l_node) {
                            String w_key = k + "-" + turn.i + "-" + turn.j + "-" + turn.l;
                            lhs15.addTerm(1.0, w.get(w_key));
                        }
                    }
                }

                // RHS: x_jl^k + y_jl^k
                IloLinearNumExpr rhs15 = cplex.linearNumExpr();
                String y_key15 = k + "-" + j_node + "-" + l_node;
                rhs15.addTerm(1.0, y.get(y_key15));
                
                String x_key15 = k + "-" + j_node + "-" + l_node;
                if (x.containsKey(x_key15)) {
                    rhs15.addTerm(1.0, x.get(x_key15));
                }
                
                cplex.addEq(lhs15, rhs15, "Turn_Link_In_" + k + "_" + j_node + "_" + l_node);
                cont15++;
            }
        }
        System.out.println("\tQtd: " + (cont14 + cont15));
        return (cont14 + cont15);
    }

    /**
     * Constrói as restrições de Quebra de Simetria.
     * @throws IloException
     */
    private int buildSymmetryBreakConstraints() throws IloException {
        System.out.print("    ... (S) Quebra de Simetria (Custo)");
        int contSym = 0;
        
        for (int k = 0; k < K - 1; k++) {
            // Z_k
            IloLinearNumExpr Z_k_expr = z_k_expressions.get(k);
            // Z_{k+1}
            IloLinearNumExpr Z_k_plus_1_expr = z_k_expressions.get(k + 1);
            
            // Cria a expressão de diferença Z_k - Z_{k+1}
            IloNumExpr symmetryConstraint = cplex.diff(Z_k_expr, Z_k_plus_1_expr);
            
            // Adiciona a restrição (Z_k - Z_{k+1}) <= 0
            cplex.addLe(symmetryConstraint, 0.0, "Symmetry_Break_" + k);
            contSym++;
        }
        System.out.println("\tQtd: " + contSym);
        return contSym;
    }

    /**
     * Anexa um callback ao CPLEX para capturar o tempo da melhor solução.
     */
    private void attachMIPInfoCallback(StopWatch stopWatch) throws IloException {
        this.bestObjectiveFound = Double.MAX_VALUE;
        this.timeToBestSolutionMillis = 0;

        cplex.use(new MIPInfoCallback() {
            @Override
            protected void main() throws IloException {
                if (hasIncumbent()) {
                    double currentObj = getIncumbentObjValue();
                    if (currentObj < bestObjectiveFound - 1e-6) {
                        bestObjectiveFound = currentObj;
                        timeToBestSolutionMillis = stopWatch.getTime(TimeUnit.MILLISECONDS);
                    }
                }
            }
        });
    }

    /**
     * Constrói o objeto Solution a partir dos resultados do CPLEX.
     * Ainda não implementado. Não montava a rota corretamente :(
     */
    public Solution buildSolution(long totalExecutionTimeMillis) throws IloException {
        Solution solution = new Solution();

        solution.setObjectiveValue((int) Math.round(cplex.getObjValue()));
        solution.setTotalExecutionTimeMillis(totalExecutionTimeMillis);
        solution.setTimeToBestSolutionMillis(this.timeToBestSolutionMillis);

        prepareServiceId();

        // Construindo as rotas
        int vehiclesUsed = 0;
        for (int k = 0; k < K; k++) {
            int realDepot = data.getRealDepotNodeId();

            vehiclesUsed++;

            Route route = new Route();
            int routeDemand = -1;

            route.addSegment(new RouteSegment("D", 0, realDepot, realDepot));
            // TODO
            route.addSegment(new RouteSegment("D", 0, realDepot, realDepot));

            int zCost = (int) Math.round(cplex.getValue(z_k_expressions.get(k)));
            route.setRouteCost(zCost);

            route.setTotalDemand(routeDemand);
            solution.addRoute(route);
        }

        solution.setNumVehiclesUsed(vehiclesUsed);
        return solution;
    }


    /**
     * Prepara um mapa para ID de serviço.
     */
    private void prepareServiceId() {
        if (this.serviceId != null) return;

        this.serviceId = new HashMap<>();
        int id = 0;

        // R_V
        for (Integer nodeId : data.getReqNodes().keySet()) {
            this.serviceId.put(nodeId + "-" + nodeId, ++id);
        }
        
        // A'_R
        for (Map.Entry<Integer, Arc> entry : data.getReqArcsOg().entrySet()) {
            Arc arc = entry.getValue();
            this.serviceId.put(arc.fromNode + "-" + arc.toNode, ++id);
        }
        
        // E_R
        for (Map.Entry<Integer, Edge> entry : data.getReqEdges().entrySet()) {
            Edge edge = entry.getValue();
            id++;
            this.serviceId.put(edge.fromNode + "-" + edge.toNode, id);
            this.serviceId.put(edge.toNode + "-" + edge.fromNode, id);
        }
    }

    /*
     * Get the remaining time in seconds
     */
    protected long getRemainingTime(StopWatch stopWatch) {
        return Math.max(TimeUnit.SECONDS.convert(MAX_RUNTIME - stopWatch.getTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS), 0);
    }

    /**
     * Salva os valores de todas as variáveis de decisão em um arquivo texto.
     */
    private void saveDecisionVariables(String filename) throws IloException {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(filename, "UTF-8")) {
            writer.println("# Valores Válidos das Variáveis de Decisão - Modelo CPLEX\n");

            writer.println("### Variáveis x_ij^k (Serviço de arco)");
            for (Map.Entry<String, IloNumVar> entry : x.entrySet()) {
                double val = cplex.getValue(entry.getValue());
                String[] kij = entry.getKey().split("-");
                String arcKey = kij[1] + "-" + kij[2];
                if (Math.abs(val) > 0.5) {
                    writer.printf("%s = %.6f - %d%n", entry.getKey(), val, this.data.getReqArcs().get(arcKey).serviceCost);
                }
            }

            writer.println("\n### Variáveis z_i^k (Serviço de nó)");
            for (Map.Entry<String, IloNumVar> entry : z.entrySet()) {
                double val = cplex.getValue(entry.getValue());
                String[] ki = entry.getKey().split("-");
                int nodeId = Integer.parseInt(ki[1]);
                if (Math.abs(val) > 0.5) {
                    writer.printf("%s = %.6f - %d%n", entry.getKey(), val, this.data.getReqNodes().get(nodeId).serviceCost);
                }
            }

            writer.println("\n### Variáveis y_ij^k (Deadheading)");
            for (Map.Entry<String, IloNumVar> entry : y.entrySet()) {
                double val = cplex.getValue(entry.getValue());
                String[] kij = entry.getKey().split("-");
                String arcKey = kij[1] + "-" + kij[2];
                if (Math.abs(val) > 0.5) {
                    writer.printf("%s = %.6f - %d%n", entry.getKey(), val, this.data.getArcs().get(arcKey).traversalCost);
                }
            }

            writer.println("\n### Variáveis f_ij^k (Fluxo de tempo)");
            for (Map.Entry<String, IloNumVar> entry : f.entrySet()) {
                String key = entry.getKey();
                double val = cplex.getValue(entry.getValue());
                int y_val = (int) Math.round(cplex.getValue(y.get(key)));

                if (x.containsKey(key) && cplex.getValue(x.get(key)) > 0.5) {
                    writer.printf("%s = %.6f - x%n", entry.getKey(), val);
                }

                if (y_val > 0) {
                    writer.printf("%s = %.6f - y%n", entry.getKey(), val);
                }
            }

            if (isTurnPenaltiesModel) {
                writer.println("\n### Variáveis w_ijl^k (Conversões)");
                for (Map.Entry<String, IloNumVar> entry : w.entrySet()) {
                    double val = cplex.getValue(entry.getValue());
                    String[] kijl = entry.getKey().split("-");
                    String turnKey = kijl[1] + "-" + kijl[2] + "-" + kijl[3];
                    if (Math.abs(val) > 0.5) {
                        writer.printf("%s = %.6f - %d%n", entry.getKey(), val, this.data.getTurns().get(turnKey).cost);
                    }
                }
            }

            writer.flush();
            System.out.println(">>> Variáveis salvas em " + filename);
        } catch (Exception e) {
            System.err.println("Erro ao salvar variáveis de decisão:");
            e.printStackTrace();
        }
    }
}
