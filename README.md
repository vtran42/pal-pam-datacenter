# Project Overview

The project focused on policy-aware data centers, wherein virtual machine traffic traverses in either ordered or unordered.

- Ordered: when the packets travel from virtual machine to virtual machine pair, it must go through middle boxes such as firewall, load balancer, and cache proxy in order.
- Unordered: when the packets travel from virtual machine to virtual machine pair, it is not neccessary travel in order between middle boxes.

In the project, we proposed two new virtual machine placement and migration problems in ordered and unordered policy-aware:

- Policy-aware virtual machine placement where virtual machines are placement in the data center in such away to give total communication cost between virtual machine and machine pair is minimum.
- Policy-aware virtual machine migration is the arrangement of virtual machines when the communication frequecies of each pair virtual machine changing. Thus, it is dynamically communicating traffic in cloud data center.

When we place or migrate the virtual machine pair, resources capacity of each Physical machine in data center is satisfied. Moreover, the algorithms in the paper are applicable for any data center architecture, cost when packets travel between switches, physical machines and switches.

# How to Run

We use fat tree topology as a data center architecture in simulation. The number of PODs can be from 2 to 30.

The project is implemented in Java with graph-stream as an external library. You can download complete graph-stream library as <http://graphstream-project.org/>

1. Download the packages from graph-stream website
2. Unzip the file download, and add the .jar file into your project as an external library in your choice IDE. In our demo, we use IntelJ.
3. Download or clone our code into your project directory and run the test.java file.

Demo:

1. Download and extract packages from graph-stream
2. Add the .jar file into the project
3. download, add source code, and run the simulation

# Publication
PAM & PAL: Policy-Aware Virtual Machine Migration and Placement in Dynamic Cloud Data Centers, IEEE International Conference on Computer Communications (Infocom 2020).
The paper can be downloaded here.

# Researchers

1. Hugo Flores
2. Vincent Tran
3. <a href="http://csc.csudh.edu/btang/"> Dr. Bin Tang </a>

# Acknowledgements

The project is sponsored by NSF Grant CNS-1911191.
