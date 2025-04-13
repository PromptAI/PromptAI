#!/bin/sh
echo "start building flow broker...."
BASE_PATH=$(cd `dirname $0`;pwd)
cd $BASE_PATH
git pull
if [ $? -ne 0 ]; then
    echo "git pull failed"
    exit 1
else 
    echo "git pull success"
fi
$BASE_PATH/gradlew -b $BASE_PATH/broker/build.gradle build
if [ $? -ne 0 ]; then
    echo "build failed"
    exit 1
else 
    echo "build success"
fi

