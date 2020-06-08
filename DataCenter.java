import org.graphstream.algorithm.Dijkstra;
import org.graphstream.algorithm.Prim;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import scala.util.parsing.combinator.testing.Str;

import java.util.*;

/**
 * <p> Data center is built on the fat tree topology. Data center includes
 * more attributes such as:
 *  - Resources: number of virtual machine in each physical machines
 *  - numMBs: number of middle boxes: firewall, epoxy,...
 *  - numVMPairs: number of virtual machines pair.
 *  - maximum communication fre: maximum communication frequency between virtual machines
 * </p>
 * <p>
 *     In the communication frequencies, these are 25% will have low communication frequency,
 *     70% middle communication frequency, and 5% high communication frequency.
 * </p>
 */
public class DataCenter extends FatTree {
    private int resources;
    private int numMBs;
    private int numVMPairs;
    private int maxCommunicationFre;
    private int[] communicationFre;
    private int[] capacity;   // capacity of each virtual machine
    // helper variable
    private Hashtable<String, Integer> mBs_Switch;	// Middle box, index location in shortest Matrix
    private Hashtable<String, Integer> VM_PM;	// Virtual Machine, physical machine index
    private double[][] shortestPathRoute;
    private List<String> ingress;
    private List<String> egress;

    //*************************************
    //******* CONSTRUCTOR *****************
    //*************************************

    /**
     * Default constructor of Data Center
     * @param numPods - Number of PODS
     * @param migrationCoef - Migration coefficient of fat tree
     * @param resources - capacity of each Physical machine
     * @param numMBs - number of MBs in the data center
     * @param numVMPairs - number pairs of virtual machine
     * @param maxCommunicationFre - maximum communication frequencies
     */
    public DataCenter(int numPods, int migrationCoef, int resources,
                      int numMBs, int numVMPairs, int maxCommunicationFre){
        super(numPods,migrationCoef);
        this.resources = resources;
        this.numMBs = numMBs;
        this.numVMPairs = numVMPairs;
        this.maxCommunicationFre = maxCommunicationFre;
        buildDataCenter();
    }

    // ******************************************
    // ******** SETTER METHOD *******************
    // ******************************************
    /**
     * Reset or changing number of PODs in data center from user
     * @param numPods - number of PODs in data center
     */
    public void setNumPods(int numPods){super.setNumPods(numPods);}
    /**
     * Reset or changing the migration coefficient of
     * fat tree topology.
     * @param migrationCoef - new migration of fat tree.
     */
    public void setMigrationCoef(int migrationCoef){
        super.setMigrationCoef(migrationCoef);
    }
    /**
     * <p>Reset or change resources of each physical machine</p>
     * @param resources - new resources or capacity of each physical machine
     */
    public void setResources(int resources){this.resources = resources;}

    /**
     * <p> Reset or change number of middle boxes</p>
     * @param numMBs - number of middle box or Virtual network function in data center.
     */
    public void setNumMBs(int numMBs){this.numMBs = numMBs;}

    /**
     * <p>Reset or change number of virtual machine pairs</p>
     * @param numVMPairs - number of virtual machine pairs
     */
     public void setNumVMPairs(int numVMPairs){this.numVMPairs = numVMPairs;}

    /**
     * <p>Reset or set up the maximum communication frequency of virtual machine pairs</p>
     * @param maxCommunicationFre - maximum communication frequency of virtual machine pairs.
     */
     public void setMaxCommunicationFre(int maxCommunicationFre){this.maxCommunicationFre = maxCommunicationFre;}

    // ******************************************
    // ******** GETTER METHOD *******************
    // ******************************************
    /**
     * Get the resources - capacity of each physical machine
     * All physical machine has the same resources
     * @return the resources of physical machine
     */
    public int getResources(){return this.resources;}

    /**
     * Get number of middle boxes
     * @return total middle boxes inside the data center
     */
    public int getNumMBs(){return this.numMBs;}

    /**
     * Get number of virtual machine pairs
     * @return number of virtual machine pair
     */
    public int getNumVMPairs(){return this.numVMPairs;}

    /**
     * Get maximum communication frequencies
     * @return the maximum communication frequency
     */
    public int getMaxCommunicationFre(){return  this.maxCommunicationFre;}

    /**
     * Get array contain the communication frequencies of each virtual machine pairs.
     * Indices of the array is indices of physical machine
     * @return the array contain communication frequencies of each virtual machine pairs.
     */
    public int[] getCommunicationFre(){return this.communicationFre;}

    /**
     * get number of virtual machine in each physical machine
     * @return array contain number of virtual machine inside each physical machine
     */
    public int[] getCapacity(){return this.capacity;}

    /**
     * Get the location of each middle box inside the data center
     * @return hash table with key is name of middle box (MB+indices), and value is indices of its switch
     */
    public Hashtable<String, Integer> getmBs_Switch(){return this.mBs_Switch;}

    /**
     * Get the location of each Virtual machine inside data center
     * @return hash table with key is name of Virtual machine (VM+indices), and value is its physical machine indices
     */
    public Hashtable<String, Integer> getVM_PM(){return this.VM_PM;}

    /**
     * <p>
     *     Get the shortest path route between two physical machines. Row and column are index of physical machines
     * </p>
     * @return The shortest path route between two physical machines in un-ordered policy,
     * row = column = number of physical machine.
     */
    public double[][] getShortestPathRoute(){return this.shortestPathRoute;}

    /**
     * Get the List of ingress in data center
     * @return List of Ingress
     */
    public List<String> getIngress(){
        setIngress();
        return this.ingress;}

    /**
     * Get the List of ingress in data center
     * @return list of Egress
     */
    public List<String> getEgress(){
        setEgress();
        return this.egress;}
    // ******************************************
    // ******** BUILD Data Center ***************
    // ******************************************
    private void buildDataCenter(){
        disMb_switch();
        distributeVM_PM(false);
        buildCommunicationFre();
        setShortestPathRoute();
//        setEgress();
//        setIngress();
    }
    /**
     * <p> Distribute the middle box into the switch randomly such that all the
     * middle boxes distributed into the aggregation switches.</p>
     * @return true if number of middle box is positive, otherwise, false
     */
    private boolean disMb_switch(){
        if(numMBs < 0){
            System.out.println("Number of Middle box cannot be negative.");
            return false;
        }
        else if(!isEnoughCapacity()){
            System.out.println("Not enough capacity");
            return false;
        }
        else{
            mBs_Switch = new Hashtable<>();
            Random r = new Random();
            List<Integer> tempList = new ArrayList<>();
            int index;
            for(int i = 0; i < numMBs; i++){
                index = r.nextInt(super.numEdgeSwitch())
                        + super.numPM() + super.numEdgeSwitch(); // choose the aggregation switch
                // check whether the aggregation switches has already contained the middle box.
                while(tempList.contains(index)){
                    index = r.nextInt(super.numEdgeSwitch())
                            + super.numPM() + super.numEdgeSwitch();
                }
                mBs_Switch.put("MB" + i, index);
                tempList.add(index);
            }
            return true;
        }
    }
    /**
     * <p>
     * Distribute the Virtual machine into the physical machine as follows:
     *  - Randomly
     *  - 80% virtual machine pairs distributed on the same edges switches.
     *  </p>
     * @param isRandom - true if distribute the virtual machine randomly, false for 80%
     */
    private void distributeVM_PM(boolean isRandom){
        Random r = new Random();
        capacity = new int[super.numPM()];    // initialize the capacity of each physical machine is 0
        VM_PM = new Hashtable<>();
        int index = 0;
        if(isRandom){
            for(int i = 0; i < numVMPairs; i++){
                index = r.nextInt(super.numPM()); // Pick the physical machine randomly
                // Virtual machine
                while(capacity[index] > resources - 1) // Verify whether physical machine has enough capacity
                    index = r.nextInt(super.numPM());
                VM_PM.put("VM" + i, index); // add the VM to physical machine
                capacity[index]++;  // update the capacity of the physical machine

                // Virtual machine Pair
                while(capacity[index] > resources - 1) // Verify whether physical machine has enough capacity
                    index = r.nextInt(super.numPM());
                VM_PM.put("VMP" + i, index); // add the VM to physical machine
                capacity[index]++;  // update the capacity of the physical machine
            }
        }
        else {
            // create the temp of hashtable es_pm
            Hashtable<Integer, List<Integer>> es_pm = new Hashtable<>(super.getEdgeSwitch_PM_Map());
            // copy all the es_pm from fat tree
            List<Integer> edgeSwitchs = new ArrayList<>();
            for(int i = 0; i < super.numEdgeSwitch(); i++){ // add the index of edge switch into the list
                edgeSwitchs.add(i);
            }
            int edgeIndex, edge, pm;
            int indexPM;
            int first80Percent = (int) (numVMPairs * 0.8);
            int second20Percent = numVMPairs - first80Percent;
            // Process for 80%
            for(int i = 0; i < first80Percent; i++, index++){
                // pick randomly index of the edge switch
                edgeIndex = r.nextInt(edgeSwitchs.size());
                edge = edgeSwitchs.get(edgeIndex);
                while(es_pm.get(edge).size() == 1 && capacity[es_pm.get(edge).get(0)] == 1){
                    edgeIndex = r.nextInt(edgeSwitchs.size()); // pick randomly index of the edge switch
                    edge = edgeSwitchs.get(edgeIndex);
                }
                // For VM
                indexPM = r.nextInt(es_pm.get(edge).size()); // pick randomly index of physical machine
                pm = es_pm.get(edge).get(indexPM);  // get a physical machine under edge switch
                VM_PM.put("VM" + index, pm);
                capacity[pm]++; // update capacity of physical machine
                if(capacity[pm] == resources){
                    es_pm.get(edge).remove(indexPM);    //remove the physical machine if its capacity reached
                }

                // For VMP
                indexPM = r.nextInt(es_pm.get(edge).size()); // pick randomly index of physical machine
                pm = es_pm.get(edge).get(indexPM);  // get a physical machine under edge switch
                VM_PM.put("VMP" + index, pm);
                capacity[pm]++; // update capacity of physical machine
                if(capacity[pm] == resources){
                    es_pm.get(edge).remove(indexPM);    //remove the physical machine if its capacity reached
                }
            }
            // Process for 20% left
            for(int i = 0; i < second20Percent; i++, index++){
                // For VM
                edgeIndex = r.nextInt(edgeSwitchs.size());  // pick randomly edge switch for VM
                edge = edgeSwitchs.get(edgeIndex);
                indexPM = r.nextInt(es_pm.get(edge).size());
                pm = es_pm.get(edge).get(indexPM);  // get indices of physical machine under edge switch
                VM_PM.put("VM" + index, pm);
                capacity[pm]++;
                if(capacity[pm] == resources)
                    es_pm.get(edge).remove(indexPM);
                if(es_pm.get(edge).size() == 0)
                    edgeSwitchs.remove(edgeIndex);
                // For VMP
                edgeIndex = r.nextInt(edgeSwitchs.size());  // pick randomly edge switch for VM
                edge = edgeSwitchs.get(edgeIndex);
                indexPM = r.nextInt(es_pm.get(edge).size());
                pm = es_pm.get(edge).get(indexPM);  // get indices of physical machine under edge switch
                VM_PM.put("VMP" + index, pm);
                capacity[pm]++;
                if(capacity[pm] == resources)
                    es_pm.get(edge).remove(indexPM);
                if(es_pm.get(edge).size() == 0)
                    edgeSwitchs.remove(edgeIndex);
            }
        }
    }
    /**
     * <p>
     *     Check whether the data center has enough capacity for number of virtual machine pairs
     * </p>
     * @return false if 2 * numVMPairs is greater than pow(numPods, 3) * resources / 4, otherwise is true
     */
    public boolean isEnoughCapacity(){
        if(2*numVMPairs > super.getNumPods() * super.getNumPods() * super.getNumPods() * resources / 4){
            System.out.println("It is not enough capacity for given number of pairs.");
            System.exit(0);
            return false;
        }
        return true;
    }
    /**
     * <p>Build or Reset the new communication frequency.
     * these are 25% will have low communication frequency,
     * 70% middle communication frequency, and 5% high communication frequency.
     * All 3 levels of communication frequencies will distribute randomly through all the array of communication
     * frequencies
     * First, we will choose a pair of virtual machine randomly, then we assign its communication frequency.
     * </p>
     */
    public void buildCommunicationFre(){
        if(!isEnoughCapacity()){
            System.out.println("Cannot create the frequency communication because it is not enough capacity");
        }
        else {
            Random r = new Random();
            this.communicationFre = new int[numVMPairs];
            int percent25,percent75, percent5;
            ArrayList<Integer> index = new ArrayList<>(); // List contain the index of virtual machine to make
            // add the indices of the vm into the array.
            for(int i = 0; i < numVMPairs; i++){
                index.add(i);
            }
            int vmIndex;    // index of vm pairs
            // Generate 25%
            percent25 = numVMPairs / 4;
            for(int i = 0; i < percent25; i++) {
                vmIndex = r.nextInt(index.size());  // choose a virtual machine randomly
                this.communicationFre[index.get(vmIndex)] = r.nextInt(301);
                index.remove(vmIndex);  // remove a chosen pair of virtual machine
            }
            // Generate 75%
            percent75 = 3 * numVMPairs / 4;
            for(int i = 0; i < percent75; i++) {
                vmIndex = r.nextInt(index.size());  // choose a virtual machine randomly
                this.communicationFre[index.get(vmIndex)] = r.nextInt(400) + 301;
                index.remove(vmIndex);  // remove a chosen pair of virtual machine
            }
            // Generate 5%
            percent5 = numVMPairs - percent25 - percent75;
            for(int i = 0; i < percent5; i++) {
                vmIndex = r.nextInt(index.size());  // choose a virtual machine randomly
                this.communicationFre[index.get(vmIndex)] = r.nextInt(300) + 701;
                index.remove(vmIndex);  // remove a chosen pair of virtual machine
            }
        }
    }

    // ******************************************
    // ******** CALCULATE THE SHORTEST PATH ROUTE ***
    // ******************************************
    /**
     * <p>Calculate the shortest path route from the source (PM) to destination PM.
     * First we build the complete graph where vertices are the set of 2 physical machines (one is source, and the
     * other is destination), and all the middle boxes. The algorithm will find the minimum spanning tree in the
     * complete graph. Then calculate the cost when travels between two physical machines such that it will visit all
     * the middle boxes. </p>
     * @param sourcePM	- index of physical machine as the source
     * @param destinationPM - index of physical machine as the destination
     * @return - the smallest weight of the walk start from a source to
     * 			a destination and visit all the middle box such as visit edges at most twice.
     */
    public double calculateSPR(int sourcePM, int destinationPM){
        double shortestPath;
        double mstWeight;
        Graph mstTree = new SingleGraph("MSTTree");
        Graph KGraph = new SingleGraph("kGraph");
        KGraph.addNode("PM"+sourcePM);
        mstTree.addNode("PM" + sourcePM);
        double shortestDistance;
        if(sourcePM != destinationPM){
            KGraph.addNode("PM" + destinationPM);
            mstTree.addNode("PM" + destinationPM);
        }
        for(int i = 0; i < numMBs; i++) {
            KGraph.addNode("MB"+i);
            mstTree.addNode("MB" + i);

            shortestDistance =
                    shortestPathMatrix[super.getListNode().get("PM"+sourcePM)][mBs_Switch.get("MB" + i)];
            KGraph.addEdge("PM"+ sourcePM + "MB" + i, "PM" + sourcePM, "MB"+i)
                    .addAttribute("weight", shortestDistance);

            mstTree.addEdge("PM"+ sourcePM + "MB" + i, "PM" + sourcePM, "MB"+i)
                    .addAttribute("length", shortestDistance);

            if(sourcePM != destinationPM) {
                shortestDistance =
                        shortestPathMatrix[super.getListNode().get("PM" + destinationPM)][mBs_Switch.get("MB" + i)];
                KGraph.addEdge("PM"+ destinationPM + "MB" + i, "PM" + destinationPM, "MB"+i)
                        .addAttribute("weight", shortestDistance);
                mstTree.addEdge("PM"+ destinationPM + "MB" + i, "PM" + destinationPM, "MB"+i)
                        .addAttribute("length", shortestDistance);
            }
        }
        // Add middle boxes
        shortestDistance = 0;
        for(int i = 0; i < numMBs-1; i++) {
            for(int j = i+1; j < numMBs; j++) {
                shortestDistance = shortestPathMatrix[mBs_Switch.get("MB"+i)][mBs_Switch.get("MB" + j)];
                KGraph.addEdge("MB" + i + "_" + j, "MB" + i, "MB" + j)
                        .addAttribute("weight", shortestDistance);
                mstTree.addEdge("MB" + i + "_" + j, "MB" + i, "MB" + j)
                        .addAttribute("length", shortestDistance);
            }
        }

        Prim mst = new Prim();
        mst.init(KGraph);
        mst.compute();

        Dijkstra dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, "length");
        dijkstra.init(mstTree);
        dijkstra.setSource(mstTree.getNode("PM" + sourcePM));
        dijkstra.compute();

        shortestPath = dijkstra.getPathLength(mstTree.getNode("PM" + destinationPM));
        mstWeight = mst.getTreeWeight();
        return 2*mstWeight - shortestPath;
    }
    /**
     * <p> Build the matrix where each entry is the cost of the path routes between 2 physical machines.</p>
     * <ul>
     *       <li> pm0 pm1 pm2 pm3 ... </li>
     *       <li>pm0</li>
     *       <li>pm1</li>
     *       <li>pm2</li>
     *       <li>.</li>
     *       <li>.</li>
     *       <li>.</li>
     * </ul>
     */
    public void setShortestPathRoute (){
        shortestPathRoute = new double[super.numPM()][super.numPM()];
        for(int i = 0; i < super.numPM(); i++) {
            for(int j = 0; j < super.numPM(); j++) {
                shortestPathRoute[i][j] = calculateSPR(i, j);
            }
        }
    }
    /**
     *<p> Display all information of data center such as number of PODs, migration coefficient, resources of each
     * physical machine, number of middle box, number of virtual machine pair, and maximum communication frequency</p>
     * <p>Display number of physical machine, number of edge switch, number of aggregation switch, and number of core
     * switch</p>
     */
    public void displayInformationOfDataCenter(){
        System.out.println("Information of Data Center");
        System.out.println("Number of Pods: " + super.getNumPods() + "\t\t\t\t\t\tMigration Coefficient: "
                + super.getMigrationCoef());
        System.out.println("Resources of each PM: " + this.resources + "\t\t\t\tNumber of Middle box: " +this.numMBs);
        System.out.println("Number of VM Pairs: " + this.numVMPairs + "\t\t\t\tMaximum Communication Frequency: "
                + this.maxCommunicationFre);
        System.out.println();
        System.out.println("Number of physical machine: " + super.numPM() + "\t\t\tNumber of Edge Switch: "
                + super.numEdgeSwitch());
        System.out.println("Number of Aggregation switch: " + super.numEdgeSwitch()
                + "\t\tNumber of Core Switch: " + super.numCoreSwitch());
    }

    // Method helper for ingress and egress

    /**
     * <p> Find the list of physical machine that close to the given middle box. </p>
     * @param mbIndex - indices of the middle box
     * @return - List of physical machine close to the given middle box.
     */
    public List<Integer> listClosestPMtoMB(int mbIndex){
        List<Integer> listPM = new ArrayList<>();
        int switchContainMB = mBs_Switch.get("MB" + mbIndex);   // get a switch contains the middle box

        ArrayList<Double> temp = new ArrayList<>();
        ArrayList<Double> tempSorted = new ArrayList<>();

        for(int i = 0; i < super.numPM(); i++){ // add the list of physical machine that connect to the switch.
            temp.add(shortestPathMatrix[switchContainMB][i]);
            tempSorted.add(shortestPathMatrix[switchContainMB][i]);
        }

        Collections.sort(tempSorted);   // Sort the temporary list in ascending order.
        double value;
        for(int i = 0; i < super.numPM(); i++){
            value = tempSorted.get(i);
            for(int j = 0; j < temp.size(); j++){
                if(temp.get(j) == value){
                    listPM.add(j);
                    temp.set(j, -1.0);
                }
            }
        }
        return listPM;
    }

    /**
     * Build the ingress list, list of Physical machines that are closest to first middle box.
     */
    public void setIngress(){
        ingress = new ArrayList<>();
        List<Integer> listClosestPM;
        listClosestPM = listClosestPMtoMB(0);
        int pmIndicies;
        int maxResources = 2 * numVMPairs;
        int i = 0;
        int index = 0;
        while(i < maxResources){
            pmIndicies = listClosestPM.get(index);  // get physical machine
            for(int j = 0; j < resources; j++){
                ingress.add(pmIndicies + "_rs" + j);
                i++;
                if(i == maxResources) break;
            }
            index++;
        }
    }

    /**
     * Build the ingress list, list of Physical machines that are closest to last middle box.
     */
    public void setEgress(){
        egress = new ArrayList<>();
        List<Integer> listClosestPM;
        listClosestPM = listClosestPMtoMB(numMBs - 1);
        int pmIndicies;
        int maxResources = 2 * numVMPairs;
        int i = 0;
        int index = 0;
        while(i < maxResources){
            pmIndicies = listClosestPM.get(index);  // get physical machine
            for(int j = 0; j < resources; j++){
                egress.add(pmIndicies + "_rs" + j);
                i++;
                if(i == maxResources) break;
            }
            index++;
        }
    }

    // Calculate communication cost in order policy

    /**
     * Calculate the length travel from first middle box to last middle box in ordered policy in data center
     * @return total length from the first to the last middle box in ordered policy.
     */
    public double costBetweenMbsOrderPolicy(){
        double cost = 0;
        int row, col;
        for(int i = 0; i < numMBs - 1; i++){
            row = mBs_Switch.get("MB" + i);
            col = mBs_Switch.get("MB" + (i+1));
            cost += shortestPathMatrix[row][col];
        }
        return cost;
    }

    /**
     * Calculate total communication frequency of all virtual machine pairs
     * @return commutative frequency of all virtual machine pairs.
     */
    public double totalCommunicationFre(){
        double total = 0;
        for (int i = 0; i < numVMPairs; i++){
            total += communicationFre[i];
        }
        return total;
    }

    /**
     * Calculate total communication cost from the first to the last middle box in ordered policy.
     * @return commutative communication cost from the first to the last middle box in ordered policy.
     */
    public double totalCommunicationCostBetweenMbsOrdered(){
        return costBetweenMbsOrderPolicy() * totalCommunicationFre();
    }
}
