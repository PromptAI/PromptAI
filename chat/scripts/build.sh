#!/usr/bin/env bash

echo "start build ..."

ROOT=$(pwd)

pnpm install
pnpm build

echo "start adjust file structure ..."

rm -rf ${ROOT}/packages/app/public/ava
if [ -f "${ROOT}/packages/app/views/sdk.js" ]; then
	rm ${ROOT}/packages/app/views/sdk.js
fi

cp ${ROOT}/packages/sdk/build/sdk.js ${ROOT}/packages/ava/build/static/
cp -R ${ROOT}/packages/ava/build ${ROOT}/packages/app/public/ava
mv ${ROOT}/packages/sdk/build/sdk.js ${ROOT}/packages/app/views/sdk.js

echo "build finished ..."