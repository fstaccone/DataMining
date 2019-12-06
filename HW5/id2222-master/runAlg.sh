#!/bin/bash
# This runs the algorithm on different graphs and generates the .txt files
bash compile.sh
bash run.sh -graph ./graphs/3elt.graph
bash run.sh -graph ./graphs/add20.graph
bash run.sh -graph ./graphs/facebook.graph
#bash run.sh -graph ./graphs/twitter.graph
#Didn't run twitter; takes too long!