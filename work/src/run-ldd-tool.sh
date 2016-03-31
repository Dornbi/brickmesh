set -e
set -x

src/build.sh

PROTO_CLASSPATH=jar/protobuf-java-3.0.0-beta-2.jar
java -cp $PROTO_CLASSPATH:jar/brickmesh.jar -Xmx128m \
  com.brickmesh.parts.LddTool $@
