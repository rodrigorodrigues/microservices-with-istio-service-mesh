FROM tiangolo/uwsgi-nginx-flask:python3.7

ARG PORT

ENV LISTEN_PORT $PORT

EXPOSE $PORT

COPY ./flask /app

WORKDIR /app

RUN pip install --upgrade pip && pip install --no-cache-dir -r ./requirements.txt