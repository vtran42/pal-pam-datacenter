import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.graph.Node;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * <p>
 *     This class provides the main structure for fat tree topology.
 * </p>
 * <p>Fat Tree will take 2 fields: number of PODs and migration coefficients.</p>
 */
public class FatTree {
    private int numPods;
    private int migrationCoef;
    private Graph fatTree;
    private Hashtable<String, Integer> listNode;
    double[][] shortestPathMatrix;

    /**
     * Constructor for FatTree Graph
     *
     * @param numPods        : number of Pod in fat tree data center, it must be even number.
     * @param migrationCoef: migration cost of the data center.
     */
    public FatTree(int numPods, int migrationCoef) {
        this.numPods = numPods;
        this.migrationCoef = migrationCoef;
        fatTree = new SingleGraph("Fat Tree");
        listNode = new Hashtable<>();
        buildFatTree();
    }

    // ******************************************
    // ******** BUILD FAT TREE GRAPH ***********
    // ******************************************

    /**
     * Add all nodes into the list nodes hash table and fat tree graph.
     * Physical machine: "PM"+index, index start 0
     * Edge Switches: "ES" + index, index start 0
     * Aggregation Switches: "AS" + index, index start 0
     * Core Switches: "CS" + index, index start 0
     *
     * @return true if number of PODS is even, otherwise return false.
     */
    private boolean addNodes() {
        if (numPods % 2 != 0) {
            System.out.println("Cannot create the Fat Tree with odd number of PODS");
            return false;
        } else {
            int index = 0;
            // add the Physical Machines
            for (int i = 0; i < numPM(); i++) {
                fatTree.addNode("PM" + i);
                listNode.put("PM" + i, index);
                index++;
            }
            // add the Edge Switches
            for (int i = 0; i < numEdgeSwitch(); i++) {
                fatTree.addNode("ES" + i);
                listNode.put("ES" + i, index);
                index++;
            }
            // add the Aggregation Switches
            for (int i = 0; i < numEdgeSwitch(); i++) {
                fatTree.addNode("AS" + i);
                listNode.put("AS" + i, index);
                index++;
            }
            // add the Core Switches
            for (int i = 0; i < numCoreSwitch(); i++) {
                fatTree.addNode("CS" + i);
                listNode.put("CS" + i, index);
                index++;
            }
            return true;
        }
    }

    /**
     * Connect the physical machines with the Edge Switch
     */
    private void addPM_ES() {
        int PMIndex = 0;
        for (int ESindex = 0; ESindex < numEdgeSwitch(); ESindex++) {
            String es = "ES" + ESindex;
            for (int i = 0; i < numPods / 2; i++) {
                String pm = "PM" + PMIndex;
                fatTree.addEdge(pm + es, pm, es).addAttribute("length", 1.0);
                PMIndex++;
            }
        }
    }

    /**
     * Connect the aggregation switches with core switches
     */
    private void addAS_CS() {
        int csIndex = 0;
        for (int ASIndex = 0; ASIndex < numEdgeSwitch(); ASIndex++) {
            String as = "AS" + ASIndex;
            for (int k = 0; k < numPods / 2; k++) {
                String cs = "CS" + csIndex;
                fatTree.addEdge(as + cs, as, cs).addAttribute("length", 1.0);
                csIndex = (csIndex < (numCoreSwitch() - 1)) ? csIndex + 1 : 0;
            }
        }
    }

    /**
     * Connect the Edge Switches with Aggregate Switches
     */
    private void addES_AS() {
        // Travel all the PODS
        for (int podIndices = 0; podIndices < numPods; podIndices++) {
            // Travel all the Edge Switches in the Pods
            for (int indexOfEdgeSwitch = numPods / 2 * podIndices;
                 indexOfEdgeSwitch < numPods / 2 * podIndices + numPods / 2;
                 indexOfEdgeSwitch++) {
                String esId = "ES" + indexOfEdgeSwitch;
                // Travel all the aggregation Switches in the Pods and connect them
                for (int indexOfAggSwitch = numPods / 2 * podIndices;
                     indexOfAggSwitch < numPods / 2 * podIndices + numPods / 2;
                     indexOfAggSwitch++) {
                    String asId = "AS" + indexOfAggSwitch;
                    fatTree.addEdge(esId + asId, esId, asId).addAttribute("length", 1.0);
                }
            }
        }
    }

    /**
     * Build the shortest path matrix with the structure as the follow:
     * physical machine + edge switch + aggregation switch + core Switch
     */
    private void buildShortestPathMatrix() {
        int dimesion = numPM() + 2 * numEdgeSwitch() + numCoreSwitch();
        shortestPathMatrix = new double[dimesion][dimesion];
        Dijkstra dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, "length");
        dijkstra.init(fatTree);
        int row, col;
        for (Node n : fatTree.getNodeSet()) {
            row = listNode.get(n.getId());
            dijkstra.setSource(n);
            dijkstra.compute();
            for (Node m : fatTree.getNodeSet()) {
                col = listNode.get(m.getId());
                shortestPathMatrix[row][col] = dijkstra.getPathLength(m);
            }
        }
    }

    public void buildFatTree() {
        if (addNodes()) {
            addPM_ES();
            addES_AS();
            addAS_CS();
            buildShortestPathMatrix();
        } else {
            System.out.println("Cannot build fat tree since k is odd.");
        }

    }

    // ******************************************
    // ******** FAT TREE GETTER***********
    // ******************************************

    /**
     * Retrieve number of PODS: k
     *
     * @return number of POds in Fat Tree
     */
    public int getNumPods() {
        return this.numPods;
    }

    /**
     * Retrieve migration coefficient of the Fat Tree
     *
     * @return migration coefficient of fat tree
     */
    public int getMigrationCoef() {
        return this.migrationCoef;
    }

    /**
     * Get the Fat Tree Graph
     *
     * @return graph of the fat tree
     */
    public Graph getFatTreeGraph() {
        return fatTree;
    }

    /**
     * Retrieve the shortest path matrix cost
     *
     * @return matix represent the shortest path between nodes
     */
    public double[][] getShortestPathMatrix() {
        return shortestPathMatrix;
    }

    /**
     * Get the list node of the fat tree
     *
     * @return hash table contain the list node of the fat tree
     * with key is name of the node
     * value is the indices of the node in the shortest path matrix
     */
    public Hashtable<String, Integer> getListNode() {
        return listNode;
    }

    // ******************************************
    // ******** FAT TREE GETTER***********
    // ******************************************

    /**
     * Get the migration coefficient of data center from user or file.
     *
     * @param migrationCoef - migration coefficient of the data center
     */
    public void setMigrationCoef(int migrationCoef) {
        this.migrationCoef = migrationCoef;
    }

    /**
     * Get number of PODs in data center from the user or file
     *
     * @param numPods - number of PODs in data center
     */
    public void setNumPods(int numPods) {
        this.numPods = numPods;
    }
    // ******************************************
    // ******** TREE GRAPH PROPERTY***********
    // ******************************************

    /**
     * Calculate number of Physical machines
     *
     * @return $\frac{\numPods^{3}}{4}$
     */
    public int numPM() {
        return numPods * numPods * numPods / 4;
    }

    /**
     * Calculate number of edge switch as well as aggregation switch
     *
     * @return $\frac{numPods^{2}}{2}$
     */
    public int numEdgeSwitch() {
        return numPods * numPods / 2;
    }

    /**
     * Calculate number of Core switch.
     *
     * @return $\frac{numPods^{2}}{4}$
     */
    public int numCoreSwitch() {
        return numPods * numPods / 4;
    }

    /**
     * Build the hash table with index of the edge switch from 0 to number edge switch as key, and list contain
     * all the physical machines that connect to edge switch.
     *
     * @return hashtable with key is the index of the edge switch, and the value is the list of the physical machines
     */
    public Hashtable<Integer, List<Integer>> getEdgeSwitch_PM_Map() {
        Hashtable<Integer, List<Integer>> ewithPM = new Hashtable<>();
        Node n;
        List<Integer> pmIndex;
        for (int i = 0; i < numEdgeSwitch(); i++) {
            pmIndex = new ArrayList<>();
            n = fatTree.getNode("ES" + i);
            for (Edge e : n.getEachEdge()) {
                int l;
                if (e.getId().contains("PM")) {
                    if (e.getNode0().getId().startsWith("PM")) {
                        l = Integer.parseInt(e.getNode0().getId().substring(2, e.getNode0().getId().length()));
                    } else {
                        l = Integer.parseInt(e.getNode1().getId().substring(2, e.getNode1().getId().length()));
                    }
                    pmIndex.add(l);
                }
            }
            ewithPM.put(i, pmIndex);
        }
        return ewithPM;
    }

    // ******************************************
    // ******** Print and display the Fat Tree **
    // ******************************************

    /**
     * Display all information about fat tree:
     * - Number of PODs
     * - Migration Coefficient
     * - List of all Nodes
     */
    public void printFatTreeInformation() {
        System.out.println("Number of PODS: " + numPods);
        System.out.println("Migration Coefficient: " + migrationCoef);
        System.out.println("List Nodes: " + listNode);
    }
}

