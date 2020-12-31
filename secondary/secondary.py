import json
import socket
from threading import Thread
from flask import Flask
import time
import sys
import random
import selectors

sel = selectors.DefaultSelector()

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

            self.listen_to_socket(s)

    def listen_to_socket(self, socket):
        conn, addr = socket.accept()
        print('Connected by', addr)
        while True:
            data = conn.recv(1024)
            print("received event %s" % data)
            if not data:
                break
            response = self.receive_data(data)
            conn.sendall(response)
        print("end of listening")
        conn.close()
        # listening again
        self.listen_to_socket(socket)

    @staticmethod
    def receive_data(data):
        # time.sleep(random.randint(3, 8))
        event = json.loads(data)
        if event["eventType"] == "add-message":
            message_list.append(event["message"])
        event_message = {
            "eventType": "ok"
        }

        print("sending message %s" % json.dumps(event_message) + "\r\n")

        return ("%s \n\r" % json.dumps(event_message)).encode()

class ThreadForSocket(Thread):
    def run(self):
        SocketServer()

ThreadForSocket().start()
