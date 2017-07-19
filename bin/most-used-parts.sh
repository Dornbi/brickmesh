#!/bin/bash
set -e
CLASSPATH=brickmesh.jar:protobuf-java-3.3.0.jar
java -cp $CLASSPATH -Xmx128m com.brickmesh.offline.MostUsedParts "$@"
