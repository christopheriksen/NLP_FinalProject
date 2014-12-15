#!/bin/sh -vx
javac -cp ./jars/lucene_core.jar:./jars/lucene_analyzer.jar:./jars/lucene_query_parser.jar ./src/*.java -d './class'
# scalac -cp ./src/*.scala -d ./class

echo "Location of file to index: "
read index_file
echo "Index folder location: "
read output_folder
echo "# of preceding sentences to include in index: "
read index_context
echo "# of numbers before and after matched entity to return: "
read query_context
echo "Number of results to return in rank: "
read num_results

java -Xms4g -Xmx4g -cp './class:./jars/*' LuceneBeta $index_file $output_folder $index_context $num_results $query_context