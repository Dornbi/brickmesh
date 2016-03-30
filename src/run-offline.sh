set -e
set -x

src/build-parts.sh

PROTO_CLASSPATH=jar/protobuf-java-3.0.0-beta-2.jar
java -cp $PROTO_CLASSPATH:jar/brickmesh.jar:jar/brickmesh-offline.jar -Xmx128m \
  com.brickmesh.offline.${1} ${@:2}

