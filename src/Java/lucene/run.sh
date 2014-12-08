#!/bin/sh -vx
javac ./src/*.java -d './class' -cp './jars/*'
java -Xms4g -Xmx4g -cp './class:./jars/*' TextFileIndexer