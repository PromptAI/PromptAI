#!/usr/bin/env bash
# quit on error
set -e

REPONAME="promptai/promptai"
#REPONAME="registry.cn-chengdu.aliyuncs.com/zpcloud/promptai"

if [ "$#" -eq 1 ]
then
    TAG=$1;
elif [ "$#" -ge 0 ]
then
    echo "Please give the release tag, e.g. v0.1, etc";
    exit 1;
fi

registry=${REGISTRY:-""}

# ROOT points to start of zp
ROOT=$(cd "$(dirname ${BASH_SOURCE[0]})"/.. && pwd)

# SAVE runtime libs for each components
LIB_DIR=$ROOT/docker/libs

# get rid of old stuff if any
rm -R -f $LIB_DIR

mkdir -p $LIB_DIR

cd ${ROOT}
echo "Build backend ..."

$ROOT/gradlew -b ./backend/build.gradle build
cp -R ./backend/build/libs/* $LIB_DIR/

echo "Build broker ..."
$ROOT/gradlew -b ./broker/build.gradle build
cp -R ./broker/build/libs/* $LIB_DIR/

echo "Build agent ..."
$ROOT/gradlew -b ./agent/build.gradle build
cp -R ./agent/build/libs/* $LIB_DIR/
#
## before build, let's pull the images that we would depend on
#docker pull promptai/promptai-ui:latest
#docker pull promptai/promptai-chat:latest
#docker pull micalabs/mica:latest

cd ${ROOT}/docker
IMAGE_NAME=${registry}${REPONAME}:${TAG}
docker build  --build-arg registry=${registry} -t ${IMAGE_NAME} -f ./Dockerfile .
docker push ${IMAGE_NAME}

echo "release done"