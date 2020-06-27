import java.util.*;

/**
 * <p> Order PAL class used for PAL for an ordered algorithm.
 *     The class will use the data center to perform calculation on those.
 * </p>
 * <p>PAL class will implement the Algorithm 1, 2, 3, and traffic aware for both ordered
 * and unordered policy.</p>
 */
public class PAL {
    private DataCenter dataCenter;
    /**
     * Default constructor
     * @param dataCenter - data center
     */
    public PAL(DataCenter dataCenter){this.dataCenter = dataCenter;}

    // ***********************************************************************************
    // *********************    ORDERED     **********************************************
    // ***********************************************************************************

    /**
     * <p> Implement Ordered PAL Algorithm 1 in paper.</p>
     * @return the total communication cost in PAL of a data center.
     */
    public double costInOrdered(){
        double cost = 0;
        Hashtable<String, Boolean> sel = new Hashtable<>(); // sel resources
        List<String> ingress = new ArrayList<>(dataCenter.getIngress());
        List<String> egress = new ArrayList<>(dataCenter.getEgress());
        for(int i = 0; i < 2 * dataCenter.getNumVMPairs(); i++){
            sel.put(ingress.get(i),false);
        }
        for(int i = 0; i < 2 * dataCenter.getNumVMPairs(); i++){
            if(sel.contains(egress.get(i))) continue;
            sel.put(egress.get(i),false);
        }

        String[] ingressArrayResourcesOptimal = new String[2 * dataCenter.getNumVMPairs()];
        String[] egressArrayResourcesOptimal = new String[2 * dataCenter.getNumVMPairs()];
        int i, j, k;
        i = k = j = 0;
        while(k  < dataCenter.getNumVMPairs()){ // Find optimal resource slots for vm and vmp
            if(sel.get(ingress.get(i))) i++;
            if(sel.get(egress.get(i))) i++;

            if(!ingress.get(i).equals(egress.get(j))){ // both optimal resources slots found
                ingressArrayResourcesOptimal[k] = ingress.get(i);
                egressArrayResourcesOptimal[k] = egress.get(j);
                sel.replace(ingress.get(i), true);
                sel.replace(egress.get(j), true);
                i++; j ++;
            }
            else{   // one found, now find another one
                int pmOfIngress = Integer.parseInt(ingress.get(i).split("_")[0]);
                int pmOfIngressNext = Integer.parseInt(ingress.get(i+1).split("_")[0]);
                int pmOfEgress = Integer.parseInt(egress.get(j).split("_")[0]);
                int pmOfEgressNext = Integer.parseInt(egress.get(j+1).split("_")[0]);
                double c1 = dataCenter.getShortestPathMatrix()[pmOfIngress][dataCenter.getmBs_Switch().get("MB" + 0)] +
                        dataCenter.getShortestPathMatrix()[pmOfEgressNext]
                                [dataCenter.getmBs_Switch().get("MB" + (dataCenter.getNumMBs() - 1))];
                double c2 = dataCenter.getShortestPathMatrix()[pmOfIngressNext][dataCenter.getmBs_Switch().get("MB" + 0)] +
                        dataCenter.getShortestPathMatrix()[pmOfEgress]
                                [dataCenter.getmBs_Switch().get("MB" + (dataCenter.getNumMBs() - 1))];
                if(c1 <= c2){
                    ingressArrayResourcesOptimal[k] = ingress.get(i);
                    egressArrayResourcesOptimal[k] = egress.get(j+1);
                    sel.replace(ingress.get(i), true);
                    sel.replace(egress.get(j+1), true);
                    i++; j += 2;
                }
                else {
                    ingressArrayResourcesOptimal[k] = ingress.get(i+1);
                    egressArrayResourcesOptimal[k] = egress.get(j);
                    sel.replace(ingress.get(i+1), true);
                    sel.replace(egress.get(j), true);
                }
            }
            k++;
        }
        // Descending order of communication frequencies
        ArrayList<Integer> afterSortFre = new ArrayList<>();
        ArrayList<Integer> original = new ArrayList<>();
        for( i = 0; i < dataCenter.getCommunicationFre().length; i++){
            afterSortFre.add(dataCenter.getCommunicationFre()[i]);
            original.add(dataCenter.getCommunicationFre()[i]);
        }
        afterSortFre.sort(Collections.reverseOrder());
        int index;
        Hashtable<String, Integer> tempVM_PM = new Hashtable<>();
        for(i = 0; i < dataCenter.getNumVMPairs();i++){
            index = original.indexOf(afterSortFre.get(i));  // index of Virtual machine will be migrated
            cost += afterSortFre.get(i) * (dataCenter.getShortestPathMatrix()
                        [Integer.parseInt(ingressArrayResourcesOptimal[i].split("_")[0])]
                        [dataCenter.getmBs_Switch().get("MB0")]
                    + dataCenter.getShortestPathMatrix()
                            [Integer.parseInt(egressArrayResourcesOptimal[i].split("_")[0])]
                            [dataCenter.getmBs_Switch().get("MB"+(dataCenter.getNumMBs() - 1))]);
        }
        return cost + dataCenter.totalCommunicationCostBetweenMbsOrdered();
    }

    public int[] ascendingOrderedOfCommunicationFre(){
        int[] indexOfVMinAscendingOrder = new int[dataCenter.getCommunicationFre().length];
        ArrayList<Integer> tempCom = new ArrayList<>(); // Temporary communication frequency list
        ArrayList<Integer> tempCom1 = new ArrayList<>();
        for(int i = 0; i < dataCenter.getCommunicationFre().length; i++){
            tempCom.add(dataCenter.getCommunicationFre()[i]);
            tempCom1.add(dataCenter.getCommunicationFre()[i]);
        }

        Collections.sort(tempCom);  // Sort temporary communication frequency in ascending order.
        int index;
        for(int i = 0; i < dataCenter.getCommunicationFre().length; i++){
            index = tempCom1.indexOf(tempCom.get(i));
            indexOfVMinAscendingOrder[i] = index;
            tempCom1.set(index,-1);
        }
        return indexOfVMinAscendingOrder;
    }

    /**
     * <p>Traffic Aware utility in ordered policy.</p>
     * <p>This will place VM pairs (in ascending order of their communication frequency) to a physical machine that are
     * closest to the ingress(first Middle Box).</p>
     * @return total communication cost in traffic aware ordered policy of a data center.
     */
    public double ultilityTrafficAwareOrdered(){
        int[] ascendingIndexOfVMPairs = ascendingOrderedOfCommunicationFre();
        List<Integer> pmClosestToFirstMb = dataCenter.listClosestPMtoMB(0);
        Hashtable<String, Integer> Vm_Pm_InTrafficAware = new Hashtable<>();
        int indexOfPm = 0;
        for(int i = 0; i < dataCenter.getNumVMPairs();){
            for(int j = 0; j < dataCenter.getResources(); j++){
                Vm_Pm_InTrafficAware.put("VM" + ascendingIndexOfVMPairs[i], pmClosestToFirstMb.get(indexOfPm));
                j++;
                Vm_Pm_InTrafficAware.put("VMP" + ascendingIndexOfVMPairs[i], pmClosestToFirstMb.get(indexOfPm));
                i++;
            }
            indexOfPm++;
        }
        double cost = 0;
        for(int i = 0; i < dataCenter.getNumVMPairs(); i++){
            cost += (dataCenter.getShortestPathMatrix()[Vm_Pm_InTrafficAware.get("VM" + i)]
                    [dataCenter.getmBs_Switch().get("MB0")]
                    + dataCenter.getShortestPathMatrix()[Vm_Pm_InTrafficAware.get("VMP" + i)]
                    [dataCenter.getmBs_Switch().get("MB" + (dataCenter.getNumMBs()-1))]+
                    dataCenter.costBetweenMbsOrderPolicy())*dataCenter.getCommunicationFre()[i];
        }
        return cost;
    }
    // ***********************************************************************************
    // *********************    UNORDERED     ********************************************
    // ***********************************************************************************

    /**
     * <p> Implement the algorithm 3 in the paper.</p>
     * @return total communication cost in unordered PAL algorithm in data center.
     */
    public double PALUnordered(){
        double cost = 0;
        Hashtable<Double,List<String>> hashX = new Hashtable<>();
        ArrayList<Double> keyList = new ArrayList<>();
        List<String> temp = new ArrayList<>();
        double key;
        for(int i = 0; i < dataCenter.numPM(); i++){
            for(int j = 0; j < dataCenter.numPM(); j++){
                key = dataCenter.getShortestPathRoute()[i][j];
                if(!keyList.contains(key)){
                    keyList.add(key);
                    temp.add(i + "_" + j);
                    hashX.put(key, temp);
                }
                else {
                    temp = new ArrayList<>(hashX.get(key));
                    temp.add(i + "_" + j);
                    hashX.put(key,temp);
                }
            }
        }
        Collections.sort(keyList);
        ArrayList<String> listPM = new ArrayList<>();
        for (Double aDouble : keyList) {
            ArrayList<String> lstemp = new ArrayList<>(hashX.get(aDouble));
            for (String s : lstemp) {
                if (!listPM.contains(s)) {
                    listPM.add(s);
                }
            }
        }

        int i = 0;
        int indexOfVM, indexOfVMP;
        int[] available = new int[dataCenter.numPM()];
        for(int index = 0; index < dataCenter.numPM(); index++){
            available[index] = dataCenter.getResources();
        }
        Hashtable<String, Integer> placement = new Hashtable<>();
        while(i< dataCenter.getNumVMPairs()){
            indexOfVM = Integer.parseInt(listPM.get(0).split("_")[0]);
            indexOfVMP = Integer.parseInt(listPM.get(0).split("_")[1]);
            do{
                if(indexOfVM != indexOfVMP){
                    if(available[indexOfVM] > 0 && available[indexOfVMP] > 0){
                        placement.put("VM" + i, indexOfVM);
                        placement.put("VMP" + i,indexOfVMP);
                        cost += dataCenter.getCommunicationFre()[i] * dataCenter.getShortestPathRoute()[indexOfVM][indexOfVMP];
                        available[indexOfVM]--; available[indexOfVMP]--;
                    }
                }
                else {
                    if(available[indexOfVM] > 1){
                        placement.put("VM" + i, indexOfVM);
                        placement.put("VMP"+ i, indexOfVMP);
                        cost += dataCenter.getCommunicationFre()[i] * dataCenter.getShortestPathRoute()[indexOfVM][indexOfVMP];
                        available[indexOfVM]--; available[indexOfVMP]--;
                    }
                }
                i++;
                if(i > dataCenter.getNumVMPairs()) break;
            }while((indexOfVM != indexOfVMP && available[indexOfVM] > 0 && available[indexOfVMP] > 0)
                || (indexOfVM == indexOfVMP && available[indexOfVM] > 1));
            listPM.remove(0);
        }
        return cost;
    }

    /**
     * <p> Implement the total communication cost in unordered PAL Traffic Aware.</p>
     * @return total communication cost in unordered PAL Traffic Aware.
     */
    public double utilityTrafficAwareUnordered(){
        double cost = 0;
        Hashtable<Double,List<String>> hashX = new Hashtable<>();
        ArrayList<Double> keyList = new ArrayList<>();
        List<String> temp = new ArrayList<>();
        double key;
        for(int i = 0; i < dataCenter.numPM(); i++){
            for(int j = 0; j < dataCenter.numPM(); j++){
                key = dataCenter.getShortestPathRoute()[i][j];
                if(!keyList.contains(key)){
                    keyList.add(key);
                }
                else {
                    temp = new ArrayList<>(hashX.get(key));
                }
                temp.add(i + "_" + j);
                hashX.put(key, temp);
            }
        }
        Collections.sort(keyList);
        ArrayList<String> listPM = new ArrayList<>();
        for(int i = 0; i < dataCenter.numPM(); i++){
            listPM.add(i +"_" +i);
        }

        for (Double aDouble : keyList) {
            ArrayList<String> lstemp = new ArrayList<>(hashX.get(aDouble));
            for (String s : lstemp) {
                if (!listPM.contains(s)) {
                    listPM.add(s);
                }
            }
        }
        // Traffic Aware start here
        int i = 0;
        int indexOfVM, indexOfVMP;
        int[] available = new int[dataCenter.numPM()];
        for(int index = 0; index < dataCenter.numPM(); index++){
            available[index] = dataCenter.getResources();
        }
        Hashtable<String, Integer> placement = new Hashtable<>();
        while(i < dataCenter.getNumVMPairs()){
            indexOfVM = Integer.parseInt(listPM.get(0).split("_")[0]);
            indexOfVMP = Integer.parseInt(listPM.get(0).split("_")[1]);
            do{
                if(indexOfVM != indexOfVMP){
                    if(available[indexOfVM] > 0 && available[indexOfVMP] > 0){
                        placement.put("VM" + i, indexOfVM);
                        placement.put("VMP" + i, indexOfVMP);
                        cost += dataCenter.getCommunicationFre()[i] * dataCenter.getShortestPathRoute()[indexOfVM][indexOfVMP];
                        available[indexOfVM]--; available[indexOfVMP]--;
                    }
                }
                else{
                    if(available[indexOfVM] > 1){
                        placement.put("VM" + i,indexOfVM);
                        placement.put("VMP" + i, indexOfVMP);
                        cost += dataCenter.getCommunicationFre()[i] * dataCenter.getShortestPathRoute()[indexOfVM][indexOfVMP];
                        available[indexOfVM]--; available[indexOfVMP]--;
                    }
                }
                i++;
                if(i > dataCenter.getNumVMPairs()) break;
            }while((indexOfVM != indexOfVMP && available[indexOfVM] > 0 && available[indexOfVMP] > 0) ||
                    (indexOfVM == indexOfVMP && available[indexOfVM] > 1));
            listPM.remove(0);
        }
        return cost;
    }
}
