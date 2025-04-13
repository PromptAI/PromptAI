#!/bin/sh
# build backend
echo "start building flow backend...."
BASE_PATH=$(cd `dirname $0`;pwd)
cd $BASE_PATH
git pull
if [ $? -ne 0 ]; then
    echo "git pull failed"
    exit 1
else 
    echo "git pull success"
fi
$BASE_PATH/gradlew --stacktrace -b $BASE_PATH/backend/build.gradle build
if [ $? -ne 0 ]; then
    echo "build failed"
    exit 1
else 
    echo "build success"
fi
