#!/bin/bash

cmd=$1
# constants
IMAGE_TAG=$(git describe --always)

if [[ -z "$cmd" ]]; then
    echo "Missing command"
    exit 1
fi

run_predictor() {
    
    IMAGE_NAME="model_predictor"
    
    model_config_path_1=$1
    model_config_path_2=$2
    port=$3
    if [[ -z "$model_config_path_1" ]]; then
        echo "Missing model_config_path_1"
        exit 1
    fi
    if [[ -z "$model_config_path_2" ]]; then
        echo "Missing model_config_path_2"
        exit 1
    fi
    if [[ -z "$port" ]]; then
        echo "Missing port"
        exit 1
    fi

    docker build -f deployment/model_predictor/Dockerfile -t model_predictor:$IMAGE_TAG .
    IMAGE_NAME=model_predictor IMAGE_TAG=$IMAGE_TAG \
        MODEL_CONFIG_PATH_1=$model_config_path_1 \
        MODEL_CONFIG_PATH_2=$model_config_path_2 PORT=$port \
        docker-compose -f deployment/model_predictor/docker-compose.yml up -d
}


run_service_predictor() {
    #docker build -f deployment/python_service/Dockerfile -t python_service:$IMAGE_TAG .
    docker build -f deployment/service_predictor/Dockerfile -t service_predictor:$IMAGE_TAG .
    IMAGE_NAME=service_predictor IMAGE_TAG=$IMAGE_TAG \
        docker-compose -f deployment/service_predictor/docker-compose.yml up -d
}

build_service_predictor() {
    # Bento service
    cd src
    bentoml build 
    bentoml containerize cancer_clf:latest --opt platform=linux/amd64
    docker tag model:5cbybmrn4oep2exp hieungo1898/bento-serviceall:latest #auto tag
    docker push hieungo1898/bento-serviceall:latest
    
    # Back end service
    docker build  --platform linux/amd64 -f deployment/service_predictor/Dockerfile -t service_predictor .
    docker tag service_predictor:latest hieungo1898/back-end:latest
    docker push hieungo1898/back-end:latest

    # flight service
    docker build  --platform linux/amd64 -f deployment/flight_service/Dockerfile -t flight_service .

}

shift

case $cmd in
run_predictor)
    run_predictor "$@"
    ;;
build_service_predictor)
    build_service_predictor "$@"
    ;;
*)
    echo -n "Unknown command: $cmd"
    exit 1
    ;;
esac


#BENTOML_CONFIG=bentoml_configuration.yml bentoml serve-grpc service2:svc --enable-reflection --debug --verbose
#mvn clean package -Dargd1=localhost
#mvn exec:java -Dexec.args="localhost"