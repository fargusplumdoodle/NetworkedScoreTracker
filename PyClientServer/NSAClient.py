import socket
import json
import threading


class NSAClient(threading.Thread):
    def __init__(self, name):
        super(NSAClient, self).__init__()

        self.port = 8989
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
            3. Once each client has responded with OK
            4. Server sends initial life total
        """
        print("%s is Waiting for game to start" % self.name)
        # 1.
        self.recv_expect("START_GAME")

        # 2.
        self.send("OK")

        # 3.
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
        # 1.
        self.recv_expect("LIFE_UPDATE")

        # 3.
        self.send("OK")

        # 4. TODO: error handling, this is bad here
        self.player_lives = json.loads(self.c.recv(1024).decode("utf-8"))

        # 5.
        self.send("OK")

        # 6.
        print("LIFE UPDATE COMPLETE for %s" % self.name)
        for p in self.player_lives:
            print("     %s:%s" % (p, self.player_lives[p]))

    def submit_life(self):
        pass

    def recv_expect(self, expected_msg):
        """
        Throws error if message is not what was expected
        :param expected_msg: a string that we are expecting from server
        """
        response = self.c.recv(self.packet_len).decode('utf-8')
        if response != expected_msg:
            raise ValueError('Error: Invalid response from server, expecting %s got %s' % (expected_msg, response))

    def send(self, msg):
        self.c.send(msg.encode('utf-8'))

    def run(self):
        while True:
            self.life_update()

    def in_game(self):
        # starting listening for new lives
        self.start()

        while True:
            # get new life from player
            new_life = input("Enter new life as a positive integer")
            print("got life: %s however we havent implemented send life function" % new_life)

            # self.submit_life(new_life)


if __name__ == '__main__':
    import sys
    try:
        c = NSAClient(sys.argv[1])
    finally:
        c.c.close()
