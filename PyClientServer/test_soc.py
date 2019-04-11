import socket
import time
import json
import threading


class NSAClient(threading.Thread):
    def __init__(self, name, port=8979):
        super(NSAClient, self).__init__()

        self.port = port
        self.secondary_port = None
        self.packet_len = 1024
        self.server_addr = ('localhost', self.port)
        self.server_addr = ('10.0.2.15', self.port)

        self.c = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.c.connect(self.server_addr)
if __name__ == '__main__':
    import sys
    try:
        c = NSAClient("BrosephJeminai", port=int(sys.argv[1]))
    finally:
        c.c.close()
