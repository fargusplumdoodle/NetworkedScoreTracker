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
        self.c = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

        self.name = name

        self.player_lives = {}

        self.join_game()
        self.start_game()

    def join_game(self):
        """
        Protocol:
            1. server listens
            2. client connects through tcp
            3. server says READY
            4. clients send the name of the player ( e.g: "Fargus" )
            5. server says OK

        The server adds the clients connection to list of connections

        The client then waits for the game to start
        """
        # 2
        self.c.connect(self.server_addr)

        # 3
        self.recv_expect("READY")

        # 4
        self.send(self.name)

        # 5
        self.recv_expect("OK")

        print('Join game protocol complete')

    def start_game(self):
        """
        Protocol:
            For each client:
                1. client listens for the START_GAME signal
                2. client says OK
            3. Server sends port for secondary line of communication
            4. secondary line of communication established
            5. Once each client has responded with OK
            6. Server sends initial life total
        """
        print("%s is Waiting for game to start" % self.name)
        # 1.
        self.recv_expect("START_GAME")

        # 2.
        self.send("OK")

        # 3.
        self.secondary_port = int(self.c.recv(1024).decode('utf-8'))

        # 3.
        self.establish_secondary_communication()

        # 4.
        self.in_game()

    def life_update(self):
        """
        Protocol:
            1. client listens for life update
            2. server says LIFE_UPDATE
            3. client says OK
            4. send each client each players life total in JSON
                - example:
                    {
                        "Isaac": 20,
                        "Liyani": 4,
                        "Dylan": 99
                    }
            5. each player responds with OK
            6. client goes to step 1.
        """

        # 2.
        self.recv_expect("LIFE_UPDATE")

        # 3.
        self.send("OK")

        # 4. TODO: error handling, this is bad here
        self.player_lives = json.loads(self.c.recv(1024).decode("utf-8"))

        # 5.
        self.send("OK")

        print("LIFE UPDATE COMPLETE for %s" % self.name)
        for p in self.player_lives:
            print("     %s:%s" % (p, self.player_lives[p]))

    def submit_life(self, new_life):
        """
        Protocol:
            1. listen for new life from client
            2. client says NEW_LIFE
            3. server says READY
            4. client sends life as a positive integer
            5. server sends new life to each player
        """
        # 2.
        self.send_line2("NEW_LIFE")

        # 3.
        self.recv_expect_line2("READY")

        # 4.
        self.send_line2(str(new_life))

        # 5.
        self.recv_expect_line2("OK")

        print("SENT NEW LIFE")

    def print_proto(self, protocol, step):
        print("Protocol: %s %s" % (protocol, step))

    def recv_expect(self, expected_msg):
        """
        Throws error if message is not what was expected
        :param expected_msg: a string that we are expecting from server
        """
        response = self.c.recv(self.packet_len).decode('utf-8')
        if response != expected_msg:
            raise ValueError('Error: Invalid response from server, expecting %s got %s' % (expected_msg, response))

    def recv_expect_line2(self, expected_msg):
        """
        Throws error if message is not what was expected
        :param expected_msg: a string that we are expecting from server
        """
        response = self.send_life_soc.recv(self.packet_len).decode('utf-8')
        if response != expected_msg:
            raise ValueError('Error: Invalid response from server, expecting %s got %s' % (expected_msg, response))

    def send(self, msg):
        self.c.send(msg.encode('utf-8'))

    def send_line2(self, msg):
        self.send_life_soc.send(msg.encode('utf-8'))

    def run(self):
        while True:
            self.life_update()

    def establish_secondary_communication(self):
        """
        Here we create a new socket for recieving life from the client so
        we dont get messages from the wrong protocol

        Protocol:
            1. server connects to client
            2. client says "NEW_LINE"
            3. server says "BOOYAH"
        """
        # 0. creating socket
        tmp_soc = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

        # Binding to address
        tmp_soc.bind(('0.0.0.0', self.secondary_port))
        tmp_soc.listen(1)  # No more than 1 connection

        print("started second line protocol on port %s" % (self.port - 1))
        # 1.
        self.send_life_soc, server_addr = tmp_soc.accept()

        # 2.
        self.send_life_soc.send("NEW_LINE".encode('utf-8'))

        # 3.
        response = self.send_life_soc.recv(self.packet_len).decode('utf-8')
        if response != "BOOYAH":
            raise ValueError('Error: Invalid response from server, expecting %s got %s' % ("NEW_LINE", response))

        print("Second line established with server")

    def in_game(self):
        # starting listening for new lives
        self.start()

        while True:
            # get new life from player
            new_life = input("Enter new life as a positive integer: ")
            self.submit_life(new_life)


if __name__ == '__main__':
    import sys
    try:
        c = NSAClient(sys.argv[1], port=int(sys.argv[2]))
    finally:
        c.c.close()
