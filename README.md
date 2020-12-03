# distributed-systems
Ucu

You should have installed sbt=1.3.13 and javac=15

run in master folder ```sbt Docker/publishLocal``` \
run in secondary folder ```sbt Docker/publishLocal```

then run master image ```docker run -d -p 8080:8080 master:0.1``` \
and secondaries image ```docker run -d -p 8081:8081 secondary:0.1``` \
```docker run -d -p 8082:8081 secondary:0.1``` \
(you can add more secondaries but then you should add their ports to master ```Main.secondariesPorts```)

run ```curl -d "{\"message\":\"1\"}" -H "Content-Type: application/json" -X POST http://localhost:8080 -D -``` to add message
run ```curl -D - localhost:8080```  to get all messages

Secondaries have sleep for 5 sec

P.S. for next iteration it will be as one sbt project and with docker compose!

**Python Secondary**
- go to secondary-py
- download python 3.8
- create venv ```python -m venv venv```
- install flask ```pip install Flask``` or ```pip install -r requirements.txt```
- run ```export FLASK_APP=secondary.py``` and ```flask run```
