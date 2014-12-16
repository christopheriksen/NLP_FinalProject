#!/bin/sh -vx
javac -cp ./jars/lucene_core.jar:./jars/lucene_analyzer.jar:./jars/lucene_query_parser.jar:stanford-corenlp-3.5.0.jar ./src/*.java -d './class'
# scalac -cp ./src/*.scala -d ./class
#
java -Xms4g -Xmx4g -cp './class:./jars/*' Lucene