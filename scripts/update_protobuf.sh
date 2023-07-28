#!/usr/bin/env bash

# This script generates the protobuffer Kotlin and Java files required for the server module, based on the definitions in the API project
# It takes one parameter, the path to the folder inside the API project from which these files are generated

set -e # fail on error

# run the script in the parent directory
BASEDIR=$(dirname "$0")
cd "$BASEDIR"/..

API_BASE_FOLDER=$1
if [[ -z $API_BASE_FOLDER ]];
then
    echo "Missing argument, please specify the full path to the protobuffer files for the API project."
    echo "Eg: update_proto.sh ~/pocketcasts-api/api/modules/protobuf/src/main/proto"
    exit 1
fi

if ! command -v "protoc" &> /dev/null; then
    echo "Error: protoc is not installed. 'brew install protobuf'"
fi

OUTPUT_FOLDER=./modules/services/protobuf/src/main/java

set -x

protoc --proto_path=$API_BASE_FOLDER/ --java_out=lite:$OUTPUT_FOLDER --kotlin_out=lite:$OUTPUT_FOLDER $API_BASE_FOLDER/api.proto
protoc --proto_path=$API_BASE_FOLDER/ --java_out=lite:$OUTPUT_FOLDER --kotlin_out=lite:$OUTPUT_FOLDER $API_BASE_FOLDER/files.proto