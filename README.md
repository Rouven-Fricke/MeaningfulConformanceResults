# Bachelor Thesis: Meaningful Conformance Checking Results

This is the readme file belonging to the bachelor's thesis "Meaningful conformance checking results" by Rouven Fricke. The repository contains the necessary files to reproduce the results that are described  in the thesis.
This readme doc supports the repo with some additional explanation regarding the code and the plugin and its usage.


# Files

The files that were supplemented to me before the beginning of the thesis as well as the code I have produces are in the BSpaceConformanceChecking folder. Other files, like the input files (event logs, petri nets, event to activity mappings etc) are also included.
In the evluation we use the file Road_Traffic_Fines_Management_Process.xes. As this is over 100mb and cannot be uploaded to GitHub, it can be found in the following link (...) and must then manually be added to the input/mwe folder.

## GraphViz

This implementation uses Graphviz 2.28 to visualize results. A config.properties file is included which specifies the directory of GraphViz. This might need to be changed depending on the machine and os the plugin is run on.


## General workflow to produce a visualization.
To start the conformance checking a caseName needs to be specified in the RouvensPlaygroundPlugin.java class. This caseName must be equal to both, the name of the .pnml petri net and the .xes log file that need to be placed in the "input/mwe" directory. If the log and petri net are used for the first time or no event to activity mapping file corresponding to the files is in the "input/etamsers" directory, the loadEtamFromSER boolean variable must be set to false to create a new mapping. Otherwise the variable can be set to true which shortens the runtime.

After the Prom application has loaded, one has to click the top middle button to get to the list of plugins and type in "Rouven's uncertain conformance checking testbed" and click on the green entry and then on start to begin the conformance checking. After the conformance checking and the construction of a deviation matrix as described in the thesis, the start visualization is shown. This may take a few moments. After that, the plugin is ready to use.

### Navigating the filtering options.
The initial view shows only one node that asks the user to specify what he wants to specify. The buttons and text fields at the top of the page can be used for this. There are a total of six fields. From the top left to the bottom right they have the following functionality.
1. The first text field allows the user to specify the number of nodes he wants to display. The maximum number is displayed at the start of the visualization in this field. The minimum number to be entered is 1.  Intervals can also be entered in the form [x, y] or x, y. This only has an effect if the corresponding interval mode is chosen.
2. The top right field lets the user choose between the two implemented modes 'Single component to partition' or 'Relation among the components of the partition'.
3. The third box lets the user specify what kind of components he wants to specify. The options 'TopN', 'BottomN' and 'Specific Interval' relate to the deviation hierarchy that is computed before. One can also specify specific components by choosing the 'Specific Components' selection. While this mode is selected, radio buttons for each component of the decomposition are shown and chooseable. With this selection, the relation among the components is shown. Other selections are overridden while the 'Specific Components' mode is chosen.
4. This box lets the user decide which component he wants to use in the 'Single Components to partition' mode as the component with outgoing edges.
5. This text field lets the user decide what edges are shown based on their weights. By doing so, the 'Exclusiveness' and 'Guaranteed co-occurrence' measures are shown. Accepted input are two decimal number between zero and one decimal place. They need to be separated by a comma and can be in square brackets. A possible input is therefore [0, 0.8] or 0.3, 1.0.
6. The 'Run with specifications' button renews the visualization according to the set parameters.


## Supplementary views
Besides a short explanation of the usage and the mapping of colors to edge weights, is a button that lets the user navigate to two supplementary views. The metrics view provides a textual representation of the 'Exclusiveness' and 'Guaranteed co-occurrence' measures on the left hand side and the hierarchy of the components on the right hand side.
The matrix view shows the deviation matrix that is used to construct the graph.

```
