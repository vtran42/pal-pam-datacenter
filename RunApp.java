import java.io.IOException;
import java.util.Scanner;

public class RunApp {
    public static DataCenter dataCenter;
    public static PAM pamAlgorithm;
    public static PAL palAlgorithm;
    public static void mainDescription(){
        System.out.println("\n***************************************************");
        System.out.println("Welcome to the PAL & PAM Project");
        System.out.println("Enter the following choices:");
        System.out.println("1. Run the PAL & PAM by default: \n" +
                "\t - Number of PODs: 8 \n" +
                "\t - Migration Coefficient of data center: 20 \n" +
                "\t - Number of middle boxes (VNFs): 3 \n" +
                "\t - Capacity(resources) of each physical machines: 20 \n" +
                "\t - Number of virtual machine pairs: 1000 \n" +
                "\t - Maximum communication Frequency: 1000");
        System.out.println("2. Enter parameters manually.");
        System.out.println("3. Enter 9 for exit: ");
        System.out.println("***************************************************");
        System.out.print("Enter your choose: ");
    }
    public static int mainMenu() throws IOException {
        int choice;
        Scanner sc = new Scanner(System.in);
        mainDescription();
        choice = sc.nextInt();
            switch (choice){
                case 1:
                    dataCenter = new DataCenter(8, 20, 20,
                            3,1000,1000);
                    break;
                case 2:
                    int numPods, migrationCoef, resources, numMBs, numVMPairs, maxCommunicationFre;
                    System.out.print("Number of PODs: ");
                    numPods = sc.nextInt();
                    System.out.print("Migration Coefficients of data center: ");
                    migrationCoef = sc.nextInt();
                    System.out.print("Capacity(resources) of each physical machines: ");
                    resources = sc.nextInt();
                    System.out.print("Number of middle boxes:  ");
                    numMBs = sc.nextInt();
                    System.out.print("Number of virtual machine pairs: ");
                    numVMPairs = sc.nextInt();
                    System.out.print("Maximum communication frequency: ");
                    maxCommunicationFre = sc.nextInt();
                    dataCenter = new DataCenter(numPods, migrationCoef, resources, numMBs,numVMPairs,maxCommunicationFre);
                    break;
            }
            dataCenter.displayInformationOfDataCenter();
            pamAlgorithm = new PAM(dataCenter);
            palAlgorithm = new PAL(dataCenter);

            comparePAMandPAL();
            displayPAL();
        sc.close();
        return choice;
    }
    public static void displayPAL(){
//        palAlgorithm = new PAL(dataCenter);
        System.out.println("************ PAL Algorithm *************");
        System.out.println("Cost in ordered PAL Algorithm 1: "+ palAlgorithm.costInOrdered());
        System.out.println("Cost in ordered PAL in traffic aware:" + palAlgorithm.ultilityTrafficAwareOrdered());
        System.out.println("Cost in unordered PAL Algorithm 3: "+ palAlgorithm.PALUnordered());
        System.out.println("Cost in unordered in Traffic aware: "+ palAlgorithm.utilityTrafficAwareUnordered());
    }
    public static void displayPAM() throws IOException {
        Scanner sc = new Scanner(System.in);
//        pamAlgorithm = new PAM(dataCenter);
        pamAlgorithm.printMCF("1");
        System.out.println("Enter the path of output file for MCF");
        String pathFile = sc.next();
        pamAlgorithm.printCommunicationCostAfterMCF(pathFile);
        System.out.println("************ PAM Algorithm *************");
        System.out.println("Total communication Cost in MCF: " + pamAlgorithm.printCommunicationCostAfterMCF(pathFile));
        System.out.println("Total communication cost in Unordered PAM: " + pamAlgorithm.costUnorderPAM());
        System.out.println("Total Communication Cost in PLAN: " + pamAlgorithm.costPLAN());

//        System.out.println("PAM\t\t" + pamAlgorithm.printCommunicationCostAfterMCF(pathFile,"2")
//                + "\t\t" + pamAlgorithm.costUnorderPAM() + "\t\t\t" + pamAlgorithm.costPLAN());
    }
    public static void comparePAMandPAL() throws IOException {
        System.out.println("******** PAM and PAL Algorithm *************");
//        System.out.println("\t\tOrdered Policy" + "\tUnordered Policy" +"\tExisting work");
//        displayPAL(12345678,12345678);
        displayPAM();
    }
    public static void main(String[] args) throws IOException {
        mainMenu();
//        DataCenter dt = new DataCenter(8,20,20,3,1000,1000);
//        dt.setIngress();
//        dt.setEgress();
//        PAL pal = new PAL(dt);
//        System.out.println("Cost in ordered PAL Algorithm 1: "+ pal.costInOrdered());
//        System.out.println("Cost in ordered PAL in traffic aware:" + pal.ultilityTrafficAwareOrdered());
//        System.out.println("Cost in unordered PAL Algorithm 3: "+ pal.PALUnordered());
//        System.out.println("Cost in unordered in Traffic aware: "+ pal.utilityTrafficAwareUnordered());
//
//        PAM pam = new PAM(dt);
    }
}