#!/bin/bash
echo '[build start]'
rm -rf build build.zip && yarn build && zip -r build.zip build
echo '[build success]'