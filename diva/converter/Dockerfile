FROM python:3.10

LABEL maintainer="nicolo.bertozzi@linksfoundation.com"
LABEL org.label-schema.schema-version = "1.0"
LABEL org.label-schema.vendor = "LINKS Foundation, Turin, Italy"
LABEL org.label-schema.description = "Converter Module"

WORKDIR /app

COPY requirements.txt /app
RUN python -m pip install -r requirements.txt

COPY /app /app

ENTRYPOINT [ "python", "main.py" ]