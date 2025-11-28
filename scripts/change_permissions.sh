#!/bin/bash

cd /home/ubuntu

# 1. 백엔드 배포 폴더가 없으면 생성 (안전장치)
mkdir -p /home/ubuntu/backend

# 2. 폴더와 그 안의 모든 파일 소유권을 ubuntu로 변경
# (CodeDeploy가 root 권한으로 이 스크립트를 실행하므로 sudo 불필요)
chown -R ubuntu:ubuntu /home/ubuntu/backend

# 3. 쓰기 권한 부여
chmod -R 755 /home/ubuntu/backend