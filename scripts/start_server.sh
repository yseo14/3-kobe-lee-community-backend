#!/bin/bash
set -e

cd /home/ubuntu/backend

ECR_REGISTRY="__ECR_REGISTRY__"
ECR_REPOSITORY="__ECR_REPOSITORY__"
IMAGE_TAG="__IMAGE_TAG__"

aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin $ECR_REGISTRY

echo "Appending ECR info to .env..."
echo "ECR_REGISTRY=$ECR_REGISTRY" >> .env
echo "ECR_REPOSITORY=$ECR_REPOSITORY" >> .env
echo "IMAGE_TAG=$IMAGE_TAG" >> .env

# 실행
docker compose pull
docker compose up -d --force-recreate
docker image prune -f