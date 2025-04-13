FROM ubuntu:24.04

# 设置非交互模式，避免安装时交互
ENV DEBIAN_FRONTEND=noninteractive

LABEL maintainer="PromptDialog <info.promptai.us>" \
      name="PromptAI Docker Package" \
      image="PromptAI" \
      vendor="PromptAI"

# 更新 apt 并安装必要的软件包
RUN apt update -y && \
    apt install -y --no-install-recommends \
        wget curl iproute2 software-properties-common \
        pkg-config libcairo2-dev openssl ca-certificates \
        gnupg2 lsb-release ubuntu-keyring libmagic-dev \
        libcurl4 libgssapi-krb5-2 libwrap0 libsasl2-2 \
        libsasl2-modules libsasl2-modules-gssapi-mit \
        liblzma5 nginx gosu && \
    rm -rf /var/lib/apt/lists/*

# 安装 Node.js, pm2
RUN curl -sL https://deb.nodesource.com/setup_lts.x | bash - && \
    apt install -y --no-install-recommends nodejs && \
    npm install -g pm2 && \
    rm -rf /var/lib/apt/lists/*

# 安装 Amazon Corretto JDK21
RUN wget -O- https://apt.corretto.aws/corretto.key | gpg --dearmor -o /usr/share/keyrings/corretto-keyring.gpg && \
    echo "deb [signed-by=/usr/share/keyrings/corretto-keyring.gpg] https://apt.corretto.aws stable main" | tee /etc/apt/sources.list.d/corretto.list && \
    apt update && apt install -y --no-install-recommends java-21-amazon-corretto-jdk && \
    rm -rf /var/lib/apt/lists/*

# 安装 MySQL
RUN apt update && apt install -y --no-install-recommends mysql-server gosu \
    && rm -rf /var/lib/apt/lists/*

# 安装 MongoDB
ARG MONGODB_VERSION=8.0.4
RUN wget -O /tmp/mongodb.tgz https://fastdl.mongodb.org/linux/mongodb-linux-x86_64-ubuntu2404-${MONGODB_VERSION}.tgz && \
    tar -zxvf /tmp/mongodb.tgz -C /usr/local/ && \
    mv /usr/local/mongodb-linux-x86_64-ubuntu2404-${MONGODB_VERSION} /usr/local/mongo && \
    ln -s /usr/local/mongo/bin/* /usr/sbin/ && \
    rm -rf /tmp/mongodb.tgz

ENV MONGO_CONFIG_DAT="/data/conf/mongod.conf" \
    MONGO_LOG_DAT="/var/log/mongo/" \
    MONGO_DATA_DAT="/data/mongo/"

# 安装mica依赖
# 安装 Python 3.9 及相关依赖
RUN apt update && apt install -y --no-install-recommends \
    software-properties-common \
    && add-apt-repository ppa:deadsnakes/ppa \
    && apt update && apt install -y --no-install-recommends \
        build-essential libffi-dev python3.9 python3.9-dev python3.9-venv \
        python3-setuptools python3-wheel python3-cffi curl \
    && update-alternatives --install /usr/bin/python3 python3 /usr/bin/python3.9 1 \
    && python3 -m ensurepip \
    && pip3 install -i https://pypi.tuna.tsinghua.edu.cn/simple --no-cache-dir --upgrade pip --break-system-packages \
        gradio==4.37.2 fastapi==0.111.0 \
        pydantic==2.7.4 langchain langchain-community pypdf beautifulsoup4 tiktoken faiss-cpu \
        uvloop ruamel.yaml  mysql-connector-python \
    && rm -rf /var/lib/apt/lists/* \
    && apt autoremove -y && apt clean

RUN apt update && apt install -y \
    &&  apt install supervisor -y

# 创建必要的目录
RUN mkdir -p /data/conf /var/log/mongo /data/mongo /usr/local/zbot /var/www/zbot /var/www/zbot_sdk \
    /usr/local/zbot/backend /usr/local/zbot/broker /usr/local/zbot/agent/logs /data/mount /data/model

# 复制 MongoDB、MySQL 和 Nginx 配置
COPY mongod.conf /data/conf/
COPY mysqld.cnf /etc/mysql/mysql.conf.d/
COPY nginx.conf /etc/nginx/nginx.conf

# 复制 UI 和 SDK
COPY --from=promptai/promptai-ui:latest /usr/share/nginx/html /var/www/promptai
COPY --from=promptai/promptai-chat:latest /var/www/promptai_chat /var/www/promptai_chat
COPY --from=micalabs/mica:latest /mica /usr/local/mica

# 安装 npm 依赖
RUN cd /var/www/promptai_chat/app && rm -rf node_modules/ && npm install

# 复制 Java 后端、Broker、Agent
COPY ./libs/promptai-backend-2.0.jar /usr/local/promptai/backend/
COPY ./libs/promptai-broker-2.0.jar /usr/local/promptai/broker/
COPY ./libs/promptai-agent-2.0.jar /usr/local/promptai/agent/

# 复制配置文件
COPY conf/*.config /data/conf/
COPY conf/*.yml /data/conf/

# 环境变量
ENV AGENT_ID=agent_a1 \
    AGENT_AK=agent_ak \
    SERVER_URL=http://127.0.0.1 \
    AI_URL=http://127.0.0.1:5001 \
    AI_BASE_DIR=/usr/local/promptai/ \
    SYSTEM_RUN_MODEL=DOCKER \
    TAG="PromptAI"

EXPOSE 80

VOLUME /data
WORKDIR /data

# 复制 Supervisor 配置和启动脚本
COPY conf/supervisord.conf /etc/supervisord.conf
COPY scripts/* /scripts/

ENTRYPOINT ["/scripts/startup.sh"]