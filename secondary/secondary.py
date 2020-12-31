import json
import socket
from threading import Thread
from flask import Flask
import time
import sys
import random

app = Flask(__name__)
message_list = []

@app.route('/')
def hello_world():
    return json.dumps(message_list)

class SocketServer:
    HOST = '127.0.0.1'
    PORT = int(sys.argv[3]) + 1 # variant of solution https://github.com/pallets/flask/issues/3095
    def __init__(self):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.bind((self.HOST, self.PORT))
            print("socket binded to %s" % self.PORT)
            s.listen(1)
            conn, addr = s.accept()
            with conn:
                print('Connected by', addr)
                while True:
                    data = conn.recv(1024)
                    print("received event %s" % data)
                    if not data:
                        break
                    response = self.receive_data(data)
                    conn.sendall(response)
                conn.close()
    @staticmethod
    def receive_data(data):
        # time.sleep(random.randint(3, 8))
        event = json.loads(data)
        print(event)
        if event["eventType"] == "add-message":
            message_list.append(event["message"])
        event_message = {
            "eventType": "ok"
        }
        return json.dumps(event_message).encode()

class ThreadForSocket(Thread):
    def run(self):
        SocketServer()

ThreadForSocket().start()
