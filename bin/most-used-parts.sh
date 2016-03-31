#!/bin/bash
set -e
CLASSPATH=brickmesh.jar:protobuf-java-3.0.0-beta-2.jar
java -cp $CLASSPATH -Xmx128m com.brickmesh.offline.MostUsedParts "$@"
