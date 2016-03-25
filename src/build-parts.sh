set -e
set -x

rm -rf gen/java/*
rm -rf jar/brickmesh*.jar
rm -rf class/core/*
rm -rf class/offline/*
rm -rf class/test/*

# Compile protos.
protoc src/proto/*.proto \
    --java_out=gen/java \
    --python_out=gen/py

# Build the part model.
export PYTHONPATH=gen/py/src/proto
src/py/build_part_model.py

PROTO_CLASSPATH=jar/protobuf-java-3.0.0-beta-2.jar

# Core.
javac -d class/core \
    -classpath "${PROTO_CLASSPATH}:class/core" \
    -Xlint:unchecked \
    src/java/com/brickmesh/parts/*.java \
    src/java/com/brickmesh/util/*.java \
    gen/java/com/brickmesh/proto/*.java

mkdir -p class/core/com/brickmesh/assets
cp src/model/*-model.txt class/core/com/brickmesh/assets
cp assets/*.txt class/core/com/brickmesh/assets
jar cf jar/brickmesh-core.jar -C class/core com

# Offline parts (not needed during runtime).
javac -d class/offline \
    -classpath "${PROTO_CLASSPATH}:jar/brickmesh-core.jar" \
    -Xlint:unchecked \
    src/java/com/brickmesh/offline/*.java
jar cf jar/brickmesh-offline.jar -C class/offline com

# Tests.
javac -d class/test \
    -classpath "${PROTO_CLASSPATH}:jar/brickmesh-core.jar:class/test" \
    -Xlint:unchecked \
    src/javatest/com/brickmesh/util/*.java

# Run unit tests & benchmarks.
TEST_CLASSPATH="${PROTO_CLASSPATH}:jar/brickmesh-core.jar:class/test"
java -cp "${TEST_CLASSPATH}" -Xmx128m com.brickmesh.util.SorterBenchmark
java -cp "${TEST_CLASSPATH}" -Xmx128m com.brickmesh.util.SorterTest

