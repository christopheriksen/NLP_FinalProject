#!/bin/sh -vx
javac -cp ./jars/lucene_core.jar:./jars/lucene_analyzer.jar:./jars/lucene_query_parser.jar ./src/*.java -d './class'
java -Xms4g -Xmx4g -cp './class:./jars/*' TextFileIndexer