# distributed-systems
Ucu

You should have installed sbt=1.3.13 and javac=15

goto master folder ```cd master```

run ```sbt Docker/publishLocal``` to build master image \
run sbt ```sbt``` and select secondary project ```project secondary``` and
 run ```Docker/publishLocal``` to build secondary image

then run master image on 8080 port ```docker run -d -p 8080:8080 master:0.1 8080 8082 8084``` \
and secondaries image with http server on 8081 and socket on 8082
 ```docker run -d -p 8081:8081 -p 8082:8082 secondary:0.1.0-SNAPSHOT 8081 8082``` \
and another secondary image with http server on 8083 and socket on 8084 and with 5 seconds sleep on post 
```docker run -d -p 8083:8083 -p 8084:8084 secondary:0.1.0-SNAPSHOT 8083 8084 -sleep``` \

run ```curl -d "{\"text\":\"test\"}" -H "Content-Type: application/json" -X POST http://localhost:8080 -D -``` to add message
run ```curl -D - localhost:8080```  to get all messages from master

**Python Secondary**
- go to secondary-py
- download python 3.8
- create venv ```python -m venv venv```
- install flask ```pip install Flask``` or ```pip install -r requirements.txt```
- run ```export FLASK_APP=secondary.py``` and ```flask run```

**Team Members: Andriy Vedilin & Diana Dmytrashko**
** Each Iteration of Task is located in related branches or tags**
