FROM python:3.8-alpine
WORKDIR /secondary
ENV FLASK_APP=app.py
ENV FLASK_RUN_HOST=0.0.0.0
RUN apk add --no-cache gcc musl-dev linux-headers
COPY requirements.txt requirements.txt
RUN pip install -r requirements.txt
EXPOSE 5000
COPY . .
CMD ["flask", "run"]



#FROM ubuntu:18.04
#RUN apt-get update -y
#RUN apt-get install -y python3-pip python3-dev build-essential
#COPY . /secondary
## RUN chmod +x secondary/docker-entrypoint.sh
#WORKDIR /secondary
#RUN pip3 install -r requirements.txt
#ENTRYPOINT ["python3", "secondary/secondary.py"]
