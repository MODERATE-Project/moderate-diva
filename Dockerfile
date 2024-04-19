FROM python:3.10

LABEL maintainer="nicolo.bertozzi@linksfoundation.com"
LABEL org.label-schema.schema-version = "1.0"
LABEL org.label-schema.vendor = "LINKS Foundation, Turin, Italy"
LABEL org.label-schema.description = "S3 Connector Module"

ENV PYTHONUNBUFFERED 1
ENV POETRY_VERSION 1.1.11

RUN pip install "poetry==$POETRY_VERSION"

WORKDIR /app

COPY pyproject.toml poetry.lock /app/

RUN poetry install
RUN pip install aiokafka
COPY /app /app

ENTRYPOINT [ "poetry", "run", "python", "-m", "main" ]