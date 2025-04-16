FROM nginx:latest
ENV APP_BASE=/var/www/

# switch to work dir
WORKDIR $APP_BASE

# prepare frontend code and build
RUN mkdir -p /var/www/promptai_chat/app
COPY ./app ./promptai_chat/
