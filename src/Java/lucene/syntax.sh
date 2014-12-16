#!/bin/sh -vx
javac -cp ./jars/lucene_core.jar:./jars/lucene_analyzer.jar:./jars/lucene_query_parser.jar:./jars/stanford-corenlp-3.5.0.jar:./jars/stanfordcorenlp-3.5.0-models.jar ./src/*.java -d './class'
# scalac -cp ./src/*.scala -d ./class
java -Xms2g -Xmx2g -cp './class:./jars/*' SyntacticModel