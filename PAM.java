import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * <p> PAM class implements the PLAN, Algorithm 4, and print out put file for Minimum Cost Flow</p>
 */
public class PAM {
    private DataCenter dataCenter;
    private double[] PLANingressEgress;
    private double[] PAMarray;
    private Hashtable<String, Integer> VM_PM_AfterMigrate;

    public PAM(DataCenter dataCenter) {
        this.dataCenter = dataCenter;
    }

    // ******************************************
    // ******** CALCULATE COST ******************
    // ******************************************

    /**
     * <p> Calculate length from first middle box to last middle box in ordered policy
     * without the communication frequencies</p>
     * @return cost in order policy from first to last middle box.
     */
    public double costBetweenMbsOrderPolicy() {
        double cost = 0;
        int row, col;
        for (int i = 0; i < dataCenter.getNumMBs() - 1; i++) {
            row = dataCenter.getmBs_Switch().get("MB" + i);
            col = dataCenter.getmBs_Switch().get("MB" + (i + 1));
            cost += dataCenter.shortestPathMatrix[row][col];
        }
        return cost;
    }

    /**
     * Calculate total communication frequencies between virtual machines pairs
     * @return total communication frequencies.
     */
    public double totalCommunicationFre() {
        double cost = 0;
        for (int i = 0; i < dataCenter.getNumVMPairs(); i++) {
            cost += dataCenter.getCommunicationFre()[i];
        }
        return cost;
    }

    /**
     * Calculate total communication cost between middle boxes with communication frequencies
     * @return total communication cost between middle boxes.
     */
    public double totalCommunicationCostBetweenMbsOrdered() {
        return costBetweenMbsOrderPolicy() * totalCommunicationFre();
    }

    /**
     * Initial communication Cost of Data Center before migration.
     * @return total communication cost of Data Center before migration.
     */
    public double initialCommunicationCostOfDatacenter() {
        double cost = 0;
        for (int i = 0; i < dataCenter.getNumVMPairs(); i++) {
            cost += (dataCenter.getShortestPathMatrix()[dataCenter.getVM_PM().get("VM" + i)]
                    [dataCenter.getmBs_Switch().get("MB0")]
                    + dataCenter.getShortestPathMatrix()[dataCenter.getVM_PM().get("VMP" + i)]
                    [dataCenter.getmBs_Switch().get("MB" + (dataCenter.getNumMBs() - 1))])
                    * dataCenter.getCommunicationFre()[i];
        }
        return cost + totalCommunicationCostBetweenMbsOrdered();
    }

    // ******************************************
    // ******** MINIMUM COST FLOW ***************
    // ******************************************
    /**
     * Print the structure format in MCF to use MCF code to calculate the MCF
     * @param fileIndex input file name for MCF file
     * @throws IOException not support
     */
    public void printMCF(String fileIndex) throws IOException {
        String name = "MCF" + fileIndex + ".inp";
        // Calculate the cost
        double[][] cost = new double[2 * dataCenter.getNumVMPairs()][dataCenter.numPM()];
        int row, col;   // use to find the cost int shortest matrix
        double migrationCost;
        double communicationCost;
        String vmType;
        for (int i = 0; i < 2 * dataCenter.getNumVMPairs(); i++) {
            vmType = (i < dataCenter.getNumVMPairs()) ? ("VM" + i) : ("VMP" + (i - dataCenter.getNumVMPairs()));
            row = dataCenter.getVM_PM().get(vmType);
            for (int j = 0; j < dataCenter.numPM(); j++) {
                col = dataCenter.getListNode().get("PM" + j);
                if (i < dataCenter.getNumVMPairs()) {
                    migrationCost = dataCenter.getMigrationCoef() * dataCenter.getShortestPathMatrix()[row][col];
                    communicationCost = dataCenter.getCommunicationFre()[i] * dataCenter.getShortestPathMatrix()[col]
                            [dataCenter.getmBs_Switch().get("MB0")];
                } else {
                    migrationCost = dataCenter.getMigrationCoef() * dataCenter.getShortestPathMatrix()[row][col];
                    communicationCost = dataCenter.getCommunicationFre()[i - dataCenter.getNumVMPairs()] *
                            dataCenter.getShortestPathMatrix()[col]
                                    [dataCenter.getmBs_Switch().get("MB" + (dataCenter.getNumMBs() - 1))];
                }
                cost[i][j] = migrationCost + communicationCost;
            }
        }
        try (PrintWriter writer = new PrintWriter(name, StandardCharsets.UTF_8)) {
            // Print to the file
            writer.println("c **** INFORMATION ABOUT DATA CENTER ****");
            writer.println("c Number of Pods (k):\t " + dataCenter.getNumPods());
            writer.println("c Migration Coefficient:\t" + dataCenter.getMigrationCoef());
            writer.println("c Number of Virtual machines Pairs: \t" + dataCenter.getNumVMPairs());
            writer.println("c Number of Middle Boxes:\t " + dataCenter.getNumMBs());
            writer.println("c Number of resources: \t" + dataCenter.getResources());
            writer.println("c Total distance between middle boxes:\t" + costBetweenMbsOrderPolicy());
            writer.println("c Total communication Frequencies:\t" + totalCommunicationFre());
            writer.println("c Cost from first middle box to last middble box:\t" + totalCommunicationCostBetweenMbsOrdered());
            writer.println("c Initial Communication Cost before migration:\t" + initialCommunicationCostOfDatacenter());
            writer.println();
            writer.println("c ***** Minimum Cost Flow *******");
            // print out all the format start here
            int totalNodes = 2 + dataCenter.getNumVMPairs() * 2 + dataCenter.numPM();
            int totalArcs = dataCenter.getNumVMPairs() * 2 + dataCenter.getNumVMPairs() * 2 * dataCenter.numPM()
                    + dataCenter.numPM();
            writer.println("p min " + totalNodes + " " + totalArcs);
            writer.println("c min-cost flow problem with " + totalNodes + " nodes and " + totalArcs + " arcs");
            writer.println("n 0 " + 2 * dataCenter.getNumVMPairs());
            writer.println("c supply of " + 2 * dataCenter.getNumVMPairs() + " at node 0 (source)");
            writer.println("n " + (totalNodes - 1) + " " + -2 * dataCenter.getNumVMPairs());
            writer.println("c demand of " + -2 * dataCenter.getNumVMPairs() + " at node " + (totalNodes - 1));

            writer.println("c arc list follows");
            writer.println("c arc has <tail> <head> <capacity l.b.> <capacity u.b> <cost>");
            writer.println();
            // Print the source to virtual machine
            for (int i = 1; i <= dataCenter.getNumVMPairs() * 2; i++) {
                writer.println("a 0 " + i + " " + 0 + " 1 " + 0);
            }
            // Print the edges from virtual machine to physical machine
            int vmIndex = 1;
            int pmIndex = dataCenter.getNumVMPairs() * 2 + 1;
            for (int r = 0; r < dataCenter.getNumVMPairs() * 2; vmIndex++, r++) {
                pmIndex = dataCenter.getNumVMPairs() * 2 + 1;
                for (int c = 0; c < dataCenter.numPM(); pmIndex++, c++) {
                    writer.println("a " + vmIndex + " " + pmIndex + " 0 1 " + cost[r][c]);
                }
            }
            int max = pmIndex;
            for (pmIndex = dataCenter.getNumVMPairs() * 2 + 1; pmIndex < max; pmIndex++) {
                writer.println("a " + pmIndex + " " + max + " 0 " + dataCenter.getResources() + " " + 0);
            }
        }
    }

    /**
     * @param fileNameIn file name for calculate communication cost in Minimum Cost Flow
     * @return - the total communication cost in MCF
     * @throws FileNotFoundException not support
     */
    public double printCommunicationCostAfterMCF(String fileNameIn) throws
            FileNotFoundException {
        FileInputStream fis = new FileInputStream(fileNameIn);
        Scanner sc = new Scanner(fis);
        String st;
        double total = 0;
        while (sc.hasNextLine()) {
            st = sc.nextLine();
            if (st.startsWith("s")) {
                String[] temp = st.split("\\s+");
                total = Integer.parseInt(temp[1]) + totalCommunicationCostBetweenMbsOrdered();
                break;
            }
        }
        sc.close();
        return total;
    }

    // ******************************************
    // ******** PAM FOR ORDERED ******************
    // ******************************************

    /**
     * Calculate the cost for unordered in PAM algorithm
     * @return cost of unordered in PAM algorithm
     */
    public double costUnorderPAM() {
        double cost = 0;
        PAMarray = new double[dataCenter.getNumVMPairs()];
        PLANingressEgress = new double[dataCenter.getNumVMPairs()];
        // Descending order of Communication Frequencies
        ArrayList<Integer> afterSortFre = new ArrayList<>();
        ArrayList<Integer> original = new ArrayList<>();
        for (int i = 0; i < dataCenter.getCommunicationFre().length; i++) {
            afterSortFre.add(dataCenter.getCommunicationFre()[i]);
            original.add(dataCenter.getCommunicationFre()[i]);
        }
        afterSortFre.sort(Collections.reverseOrder());
        VM_PM_AfterMigrate = new Hashtable<>();
        VM_PM_AfterMigrate.putAll(dataCenter.getVM_PM());
        int[] communicationFre = new int[dataCenter.getCommunicationFre().length];
        for (int i = 0; i < communicationFre.length; i++) {
            communicationFre[i] = dataCenter.getCommunicationFre()[i];
        }
        double c_min, c_ij;
        //		Hashtable<String, Integer> m = new Hashtable<String, Integer>();
        int[] available = new int[dataCenter.numPM()];
        for (int i = 0; i < dataCenter.numPM(); i++)
            available[i] = dataCenter.getCapacity()[i];
        int index;
        double c_i, c_j;
        int a, b;
        for (int k = 0; k < dataCenter.getNumVMPairs(); k++) {
            index = original.indexOf(afterSortFre.get(k)); // index of VM that will be migrated
            c_min = Double.MAX_VALUE;    // minimum total cost for (v_k, v'_k)
            a = -1;
            b = -1;
            for (int i = 0; i < dataCenter.getNumVMPairs(); i++) { // find PM pairs for VM Pair (v_k, v'_k)
                for (int j = i; j < dataCenter.numPM(); j++) {
                    if (available[j] == 0 || (i == j && available[j] < 2)) continue;
                    c_i = dataCenter.getMigrationCoef() * dataCenter.getShortestPathMatrix()
                            [VM_PM_AfterMigrate.get("VM" + index)][dataCenter.getListNode().get("PM" + i)];
                    c_j = dataCenter.getMigrationCoef() * dataCenter.getShortestPathMatrix()
                            [VM_PM_AfterMigrate.get("VMP" + index)][dataCenter.getListNode().get("PM" + i)];
                    c_ij = communicationFre[index] * dataCenter.getShortestPathRoute()[i][j] + c_i + c_j;
                    if (c_ij < c_min) {
                        a = i;
                        b = j;
                        c_min = c_ij;
                    }
                }
            }
            VM_PM_AfterMigrate.replace("VM" + index, a);
            VM_PM_AfterMigrate.replace("VMP" + index, b);
            cost += c_min;
            available[a]--;
            available[b]--;
            original.set(index, -1);
        }
        return cost;
    }
    // *******************************************
    // ****************** PLAN *******************
    // *******************************************

    /**
     * Matrix represents the shortest distance from one middle box
     * to all the middle boxes
     * row [mb0, mb1, mb2, ...]
     * column [mb0, mb1, mb2,...]
     * @return double array represents the shortest distance between middle boxes.
     */
    public double[][] middleBoxMatrix() {
        int numMBs = dataCenter.getNumMBs();
        double[][] cost = new double[numMBs][numMBs];
        for (int i = 0; i < numMBs; i++) {
            for (int j = 0; j < numMBs; j++) {
                cost[i][j] = dataCenter.getShortestPathMatrix()[dataCenter.getmBs_Switch().get("MB" + i)]
                        [dataCenter.getmBs_Switch().get("MB" + j)];
                //				System.out.print(cost[i][j] + "\t");
            }
            //			System.out.println();
        }
        return cost;
    }

    /**
     * Get the list ordered travel between middle boxes with the
     * minimum distance between middle boxes.
     * @param ingress - source as a middle box closest to VM
     * @param egress  - destination as a middle box closest to VMP
     * @return - list in order of Middle box
     */
    public List<Integer> orderTravelMB(int ingress, int egress) {
        List<Integer> isAdded = new ArrayList<>();
        int index = ingress, temp = -1;
        double min = Double.MAX_VALUE;
        int numMBs = dataCenter.getNumMBs();
        double[][] mb = new double[numMBs][numMBs];
        for (int i = 0; i < numMBs; i++) {
            for (int j = 0; j < numMBs; j++)
                mb[i][j] = middleBoxMatrix()[i][j];
        }
        isAdded.add(ingress);
        for (int i = 0; i < numMBs - 1; i++) {
            for (int j = 0; j < numMBs; j++) {
                if (!isAdded.contains(j)) {
                    if (min > mb[index][j]) {
                        min = mb[index][j];
                        temp = j;
                    }
                }
            }
            index = temp;
            isAdded.add(index);
            min = Double.MAX_VALUE;
        }
        if (!isAdded.contains(egress))
            isAdded.add(egress);
        return isAdded;
    }

    /**
     * Find the ingress: the middle box closest to the VM,
     * and egress: the middle box closest to the VMP
     * @param pm - index of the physical machine
     * @return the index of the middle box
     */
    public int ingressOrEgressMB(int pm) {
        int mb = -1;    // index of the middle box
        double min = Double.MAX_VALUE;
        for (int i = 0; i < dataCenter.getNumMBs(); i++) {
            if (min > dataCenter.getShortestPathMatrix()[dataCenter.getmBs_Switch().get("MB" + i)]
                    [dataCenter.getListNode().get("PM" + pm)]) {
                min = dataCenter.getShortestPathMatrix()[dataCenter.getmBs_Switch().get("MB" + i)]
                        [dataCenter.getListNode().get("PM" + pm)];
                mb = i;
            }
        }
        return mb;
    }

    /**
     * Calculate the cost in PLAN algorithm of unordered PAL (without migration)
     * travel all the Virtual machines pair
     * for each virtual machine pair, find the ingress and egress middle box
     * calculate the length from VM to ingress, and VMP to egress
     * calculate the total communication from VM to ingress, ingress to all middle box,
     * and VMP to egress
     * @return  cost in PLAN Algorithm
     */
    public double costPLAN() {
        // Print to the file
        double communicationCost = 0;
        int ingress, egress;
        int numVMPairs = dataCenter.getNumVMPairs();
        PLANingressEgress = new double[numVMPairs];
        List<Integer> travel;
        double shortestPath;
        // processes for all Virtual machine pairs
        for (int i = 0; i < numVMPairs; i++) {
            ingress = ingressOrEgressMB(dataCenter.getVM_PM().get("VM" + i));    // find the ingress of VM
            egress = ingressOrEgressMB(dataCenter.getVM_PM().get("VMP" + i));    // find the egress of VMP
            shortestPath = dataCenter.getShortestPathMatrix()[dataCenter.getmBs_Switch().get("MB" + ingress)]
                    [dataCenter.getVM_PM().get("VM" + i)] +
                    dataCenter.getShortestPathMatrix()[dataCenter.getmBs_Switch().get("MB" + egress)]
                            [dataCenter.getVM_PM().get("VMP" + i)];
            // initialize or reset the travel list
            travel = new ArrayList<>(orderTravelMB(ingress, egress));    // add all the middle box to the travel list
            double shortestPathMB = 0;
            int firstMB, secondMB = -1;
            int numMBs = dataCenter.getNumMBs();
            for (int j = 0; j < numMBs - 1; j++) {
                firstMB = travel.get(j);    // get the source middle box in the list
                secondMB = travel.get(j + 1);    // get the destination middle box in the list
                shortestPathMB += dataCenter.getShortestPathMatrix()[dataCenter.getmBs_Switch().get("MB" + firstMB)]
                        [dataCenter.getmBs_Switch().get("MB" + secondMB)];
            }
            if (dataCenter.getVM_PM().get("VM" + i).equals(dataCenter.getVM_PM().get("VMP" + i)))
                shortestPathMB += dataCenter.getShortestPathMatrix()[dataCenter.getmBs_Switch().get("MB" + ingress)]
                        [dataCenter.getmBs_Switch().get("MB" + secondMB)];
            communicationCost = communicationCost + dataCenter.getCommunicationFre()[i] * (shortestPath + shortestPathMB);
            PLANingressEgress[i] = dataCenter.getCommunicationFre()[i] * (shortestPath + shortestPathMB);
        }
        return communicationCost;
    }
}
