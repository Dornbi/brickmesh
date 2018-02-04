set -e
set -x

src/build.sh

PROTO_CLASSPATH=../bin/protobuf-java-3.3.0.jar
java -cp $PROTO_CLASSPATH:jar/brickmesh.jar -Xmx128m \
  com.brickmesh.parts.Ldd2Wanted $@
