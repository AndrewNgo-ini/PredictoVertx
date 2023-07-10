FROM python:3.9-slim

RUN apt-get update

RUN apt-get install -y gcc

RUN python -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

COPY deployment/python_service/requirements.txt .
RUN pip install --upgrade wheel setuptools pip
RUN pip install -r requirements.txt

COPY ./src /sample_solution/src
WORKDIR /sample_solution
