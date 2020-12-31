# distributed-systems

**Team Members: Andriy Vedilin & Diana Dmytrashko** \
**Each Iteration of Task is located in related branches or tags**

You should have installed sbt=1.3.13 and javac=15, and python=3.8

**Scala Master** 
- goto master folder ```cd master``` 
- start ```sbt``` 
- run master in sbt with ports ```run 8080 8082 8084``` 
    - first arg is port for http server 
    - second arg is socket port for secondary
    - third arg is socket for another secondary

**Scala Secondary**
- goto master folder ```cd master``` 
- start ```sbt``` 
- select secondary sub-project ```project secondary```
- run secondary in sbt with ports ```run 8081 8082 ```
    - first arg is port for http server 
    - second arg is socket port

**Python Secondary**
- go to secondary
- install flask ```pip install Flask``` or ```pip install -r requirements.txt```
- run ```export FLASK_APP=secondary.py``` and ```flask run -p 8081```
  - flask http will be running on port 8081 and socket on 8082 \
  P.S. it's not a trivial task to pass flask server additional arguments 

**Docker support**
It's possible to create docker image for python and scala (```Docke/publishLocal``` in sbt per each project) apps, 
but scala app is not responding when it's running, we need some time to fix it
