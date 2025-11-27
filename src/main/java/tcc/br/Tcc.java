package tcc.br;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.time.StopWatch;

public class Tcc {

    private String instanceName;
    private int numVehicles;
    private int capacity;           // Tmax
    private int depotNode;

    private int numNodes;
    private int numEdges;
    private int numArcs;
    private int numRequiredNodes;
    private int numRequiredEdges;
    private int numRequiredArcs;
    private int numTurns;

    private Map<Integer, Node> reqNodes;
	private Map<Integer, Node> nonReqNodes;
    private Map<Integer, Edge> reqEdges;
	private Map<Integer, Edge> nonReqEdges;
    private Map<Integer, Arc> reqArcs;
	private Map<Integer, Arc> nonReqArcs;
    private Map<String, Turn> turns;

    public Tcc() {
        this.reqNodes = new HashMap<>();
		this.nonReqNodes = new HashMap<>();
        this.reqEdges = new HashMap<>();
		this.nonReqEdges = new HashMap<>();
        this.reqArcs = new HashMap<>();
		this.nonReqArcs = new HashMap<>();
        this.turns = new HashMap<>();
    }

    private void addNode(int id, Node node) {
        if (node.isRequired) {
            this.reqNodes.put(id, node);
        } else {
            this.nonReqNodes.put(id, node);
        }
    }

    private void addEdge(int id, Edge edge) {
        if (edge.isRequired) {
            this.reqEdges.put(id, edge);
        } else {
            this.nonReqEdges.put(id, edge);
        }
    }

    private void addArc(int id, Arc arc) {
        if (arc.isRequired) {
            this.reqArcs.put(id, arc);
        } else {
            this.nonReqArcs.put(id, arc);
        }
    }

    /**
     * Lê o arquivo de entrada baseado no tipo (NEARP ou NEARP-TP).
     */
    public void readInput(String inputFilePath, String inputType) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(inputFilePath))) {
            // Cabeçalho
            parseHeader(br);

            // Restante do arquivo baseado no tipo
            if ("NEARP".equalsIgnoreCase(inputType)) {
                parseNearp(br);
            } else if ("NEARPTP".equalsIgnoreCase(inputType)) {
                parseNearpTp(br);
            } else {
                throw new IllegalArgumentException("Tipo de entrada desconhecido: " + inputType);
            }

        } catch (IOException e) {
            throw new IOException("Erro ao ler o arquivo de entrada", e);
        } catch (Exception e) {
            throw new IOException("Erro ao processar os dados do arquivo de entrada", e);
        }
    }

    /**
     * Lê o cabeçalho (metadados) do arquivo de entrada.
     */
    private void parseHeader(BufferedReader br) throws IOException {
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {       // cabeçalho termina com linha em branco
                break;
            }

            String[] parts = line.split(":\\s+", 2);
            if (parts.length < 2) continue; // linha não é um par chave-valor

            String key = parts[0].trim();
            String value = parts[1].trim();

            switch (key) {
                case "Name":
                    this.instanceName = value;
                    break;
                case "#Vehicles":
                    this.numVehicles = Integer.parseInt(value);
                    break;
                case "Capacity":
                    this.capacity = Integer.parseInt(value);
                    break;
                case "Depot Node":
                case "Depot":
                    this.depotNode = Integer.parseInt(value);
                    break;
                case "#Nodes":
                    this.numNodes = Integer.parseInt(value);
                    break;
                case "#Edges":
                    this.numEdges = Integer.parseInt(value);
                    break;
                case "#Arcs":
                    this.numArcs = Integer.parseInt(value);
                    break;
                case "#Required-N":
                case "#Required N":
                    this.numRequiredNodes = Integer.parseInt(value);
                    break;
                case "#Required-E":
                case "#Required E":
                    this.numRequiredEdges = Integer.parseInt(value);
                    break;
                case "#Required-A":
                case "#Required A":
                    this.numRequiredArcs = Integer.parseInt(value);
                    break;
                case "#Nb-Turns":
                    this.numTurns = Integer.parseInt(value);
                    break;
            }
        }
    }

    /**
     * Parsing para o formato NEARP.
     */
    private void parseNearp(BufferedReader br) throws IOException {
        // --- NODES ---
        br.readLine();      // "ReN.    DEMAND  S. COST"

        // Nós requeridos
        for (int i = 0; i < this.numRequiredNodes; i++) {
            String[] parts = br.readLine().trim().split("\t");
            int id = Integer.parseInt(parts[0].replaceAll("\\D+", ""));
			Node n = new Node(
				Integer.parseInt(parts[1]), 
				Integer.parseInt(parts[2]), 
				true
			);
			addNode(id, n);
        }

		// Nós não requeridos
		for (int i = 1; i <= this.numNodes; i++) {
			if (!reqNodes.containsKey(i)) {
				addNode(i, new Node());
			}
        }

        // --- EDGES ---
		br.readLine();		// linha em branco
        br.readLine();      // "ReE. From N. To N.   T. COST DEMAND  S. COST"
        
		// Arestas requeridas
        for (int i = 0; i < this.numRequiredEdges; i++) {
            String[] parts = br.readLine().trim().split("\t");
            int id = Integer.parseInt(parts[0].replaceAll("\\D+", ""));
            Edge e = new Edge(
                Integer.parseInt(parts[1]),     // from
                Integer.parseInt(parts[2]),     // to
                Integer.parseInt(parts[3]),     // traversal
                Integer.parseInt(parts[4]),     // demand
                Integer.parseInt(parts[5]),     // service
                true
            );
            addEdge(id, e);
        }
        
		br.readLine();		// linha em branco
        br.readLine();      // "EDGE    FROM N. TO N.   T. COST"
        
		// Arestas não requeridas
        for (int i = 0; i < (this.numEdges - this.numRequiredEdges); i++) {
            String[] parts = br.readLine().trim().split("\t");
            int id = Integer.parseInt(parts[0].replaceAll("\\D+", ""));
            Edge e = new Edge(
                Integer.parseInt(parts[1]),		// from
                Integer.parseInt(parts[2]),		// to
                Integer.parseInt(parts[3]),		// traversal
                0,						// demand
                0,					// service
                false
            );
            addEdge(id, e);
        }

        // --- ARCS ---
		br.readLine();		// linha em branco
        br.readLine();		// "ReA.	FROM N.	TO N.	T. COST	DEMAND	S. COST"

		// Arcos requeridos
        for (int i = 0; i < this.numRequiredArcs; i++) {
            String[] parts = br.readLine().trim().split("\t");
            int id = Integer.parseInt(parts[0].replaceAll("\\D+", ""));
            Arc a = new Arc(
				Integer.parseInt(parts[1]),		// from
				Integer.parseInt(parts[2]),		// to
				Integer.parseInt(parts[3]),		// traversal
				Integer.parseInt(parts[4]),		// demand
				Integer.parseInt(parts[5]),		// service
				true
            );
            addArc(id, a);
        }

		br.readLine();		// linha em branco
        br.readLine();		// "ARC	FROM N.	TO N.	T. COST"

		// Arcos não requeridos
        for (int i = 0; i < (this.numArcs - this.numRequiredArcs); i++) {
            String[] parts = br.readLine().trim().split("\t");
            int id = Integer.parseInt(parts[0].replaceAll("\\D+", ""));
            Arc a = new Arc(
				Integer.parseInt(parts[1]),		// from
				Integer.parseInt(parts[2]),		// to
				Integer.parseInt(parts[3]),		// traversal
				0,						// demand
				0,					// service
				false
            );
            addArc(id, a);
        }
    }

	/**
     * Parsing para o formato NEARP-TP.
     */
	private void parseNearpTp(BufferedReader br) throws IOException {
		br.readLine();		// "----------NODES----------"
		br.readLine();		// "INDEX	QTY	IS-REQUIRED	X	Y"

		for (int i = 0; i < this.numNodes; i++) {
            String[] parts = br.readLine().trim().split("\t");
			int id = Integer.parseInt(parts[0]);
            Node n = new Node(
				0,
				Integer.parseInt(parts[1]),				// qty (serviceCost)
				Integer.parseInt(parts[2]) == 1			// isRequired
            );
			addNode(id, n);
        }

		br.readLine();		// linha em branco
		br.readLine();		// "----------EDGES----------"
		br.readLine();		// "INDEX-I	INDEX-J	QTY	IS-REQUIRED	TR-COST"

        for (int i = 1; i <= this.numEdges; i++) {
            String[] parts = br.readLine().trim().split("\t");
            Edge e = new Edge(
				Integer.parseInt(parts[0]),			// from (I)
				Integer.parseInt(parts[1]),			// to (J)
				Integer.parseInt(parts[4]),			// tr-cost
				0,
				Integer.parseInt(parts[2]),			// qty (service cost)
				Integer.parseInt(parts[3]) == 1		// isRequired
            );
			addEdge(i, e);
        }

		br.readLine();		// linha em branco
		br.readLine();		// "-----------ARCS----------"
		br.readLine();		// "INDEX-I INDEX-J QTY IS-REQUIRED TR-COST"

        for (int i = 1; i <= this.numArcs; i++) {
            String[] parts = br.readLine().trim().split("\t");
            Arc a = new Arc(
				Integer.parseInt(parts[0]), 		// from (I)
				Integer.parseInt(parts[1]), 		// to (J)
				Integer.parseInt(parts[4]), 		// tr-cost
				0,
				Integer.parseInt(parts[2]), 		// qty (service cost)
				Integer.parseInt(parts[3]) == 1 	// isRequired
            );
            addArc(i, a);
        }

		br.readLine();		// linha em branco
		br.readLine();		// "----------TURNS----------"
		br.readLine();		// "INDEX-I	INDEX-J	INDEX-K	COST	TYPE"

		for (int i = 0; i < this.numTurns; i++) {
			String[] parts = br.readLine().trim().split("\t");
            String key = parts[0] + "-" + parts[1] + "-" + parts[2];
			Turn t = new Turn(
				Integer.parseInt(parts[0]), 		// I
				Integer.parseInt(parts[1]), 		// J
				Integer.parseInt(parts[2]), 		// K
				Integer.parseInt(parts[3]), 		// cost
				parts[4]  							// type
			);
			this.turns.put(key, t);
		}
    }

    /**
     * Escreve o objeto Solution em um arquivo de saída, no formato especificado.
     * @param sol O objeto Solution preenchido.
     * @param outputFilePath O caminho do arquivo a ser escrito.
     */
    public void writeOutput(Solution sol, String outputFilePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            // Linha 1: Função Objetivo
            writer.write(String.valueOf(sol.getObjectiveValue()));
            writer.newLine();

            // Linha 2: Número de Rotas (Veículos Usados)
            writer.write(String.valueOf(sol.getNumVehiclesUsed()));
            writer.newLine();

            // Linha 3: Tempo Total de Execução (ms)
            writer.write(String.valueOf(sol.getTotalExecutionTimeMillis()));
            writer.newLine();

            // Linha 4: Tempo para Melhor Solução (ms)
            writer.write(String.valueOf(sol.getTimeToBestSolutionMillis()));
            writer.newLine();

            // Linhas 5+: Detalhes da Rota
            int routeIndex = 1;
            for (Route route : sol.getRoutes()) {
                StringBuilder lineBuilder = new StringBuilder();

                // Espaço no início da linha
                lineBuilder.append(" ");

                // 1. Índice do Depósito Real
                lineBuilder.append(this.depotNode);
                lineBuilder.append(" ");

                // 2. Dia
                lineBuilder.append(1);
                lineBuilder.append(" ");

                // 3. Índice da Rota
                lineBuilder.append(routeIndex++);
                lineBuilder.append(" ");

                // 4. Demanda Total da Rota
                lineBuilder.append(route.getTotalDemand());
                lineBuilder.append(" ");

                // 5. Custo da Rota
                lineBuilder.append(route.getRouteCost());
                lineBuilder.append(" ");

                // 6. Número de Visitas (Contagem de 'D' e 'S')
                int numVisits = 0;
                for (RouteSegment segment : route.getSegments()) {
                    if ("D".equals(segment.getType()) || "S".equals(segment.getType())) {
                        numVisits++;
                    }
                }
                lineBuilder.append(numVisits);

                // 7. Lista de Segmentos da Rota
                for (RouteSegment segment : route.getSegments()) {
                    lineBuilder.append(" ");
                    lineBuilder.append(segment.toString());
                }
                
                writer.write(lineBuilder.toString());
                writer.newLine();
            }

        } catch (IOException e) {
            System.err.println("Erro ao escrever o arquivo de saída: " + outputFilePath);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Start the stopwatch to track the running time
        StopWatch stopWatch = StopWatch.createStarted();

        if (args.length != 3) {
            System.out.println("Usage: java -jar target/tcc-1.0.jar <inputType> <inputFilePath> <outputFilePath>");
            System.out.println("\tinputType: NEARP ou NEARPTP");
            return;
        }

        String inputType = args[0];
        String inputFilePath = args[1];
        String outputFilePath = args[2];

        Tcc tcc = new Tcc();
        try {
            tcc.readInput(inputFilePath, inputType);
            System.out.println("\nInstância lida com sucesso: " + tcc.instanceName);

			// Log de verificação
            System.out.println("\nTotal de Nós: " + (tcc.reqNodes.size() + tcc.nonReqNodes.size()));
            System.out.println("Total de Arestas: " + (tcc.reqEdges.size() + tcc.nonReqEdges.size()));
            System.out.println("Total de Arcos: " + (tcc.reqArcs.size() + tcc.nonReqArcs.size()));
            System.out.println("Total de Conversões: " + tcc.turns.size());

            Map<String, Integer> contagemTipos = new HashMap<>();
            for (Turn t : tcc.turns.values()) {
                contagemTipos.put(t.type, contagemTipos.getOrDefault(t.type, 0) + 1);
            }

            System.out.println("  Quantidade por tipo:");
            for (Map.Entry<String, Integer> entry : contagemTipos.entrySet()) {
                System.out.println("  - " + entry.getKey() + ": " + entry.getValue());
            }

			System.out.println("\nIniciando pré-processamento...");
            
            TccPreProcessing preProcessor = new TccPreProcessing(
                tcc.numVehicles, tcc.capacity, tcc.depotNode,
				tcc.reqNodes, tcc.nonReqNodes,
                tcc.reqEdges, tcc.nonReqEdges,
                tcc.reqArcs, tcc.nonReqArcs,
                tcc.turns
            );

            CplexData cplexData = preProcessor.getCplexData();

            System.out.println("Pré-processamento concluído.");
            System.out.println("  Número de veículos: " + cplexData.getNumVehicles());
            System.out.println("  Capacidade: " + cplexData.getCapacity());
            System.out.println("  Total de nós (N): " + cplexData.getNodes().size());
			System.out.println("  Total de nós requeridos (R_V): " + cplexData.getReqNodes().size());
            System.out.println("  Total de arcos (A): " + cplexData.getArcs().size());
			System.out.println("  Total de arcos originais requeridos (A'_R): " + cplexData.getReqArcsOg().size());
			System.out.println("  Total de arestas requeridas (E_R): " + cplexData.getReqEdges().size());
            System.out.println("  Total de arcos requeridos (R_A): " + cplexData.getReqArcs().size());

            System.out.println("\n  Quantidade de variáveis contínuas: " + (cplexData.getNumVehicles() * cplexData.getArcs().size()));
            System.out.println("  Quantidade de variáveis inteiras: " + (cplexData.getNumVehicles() * (cplexData.getArcs().size() + cplexData.getTurns().size())));
            System.out.println("  Quantidade de variáveis binárias: " + (cplexData.getNumVehicles() * (cplexData.getReqArcs().size() + cplexData.getReqNodes().size())));
            
            System.out.println("\nInicializando o modelo CPLEX...");
            
            CplexModel model = new CplexModel(cplexData, inputType);
            Solution solution = model.solve(stopWatch);

            if (solution != null) {
                System.out.println("Solução encontrada! Escrevendo saída...");
                tcc.writeOutput(solution, outputFilePath);
            } else {
                System.out.println("Nenhuma solução foi retornada.");
            }

        } catch (IOException e) {
            System.err.println("Falha ao ler ou processar a instância.");
            e.printStackTrace();
        }
    }
}