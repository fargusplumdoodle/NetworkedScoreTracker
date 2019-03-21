import socket


class NSAClient(object):
    def __init__(self, name):
        self.port = 7989
        self.packet_len = 1024
        self.server_addr = ('localhost', self.port)
        self.c = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

        self.name = name

        self.join_game()

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
        pass

    def life_update(self):
        pass

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
        msg = msg.encode('utf-8')
        self.c.send(msg)

if __name__ == '__main__':
    import sys
    try:
        c = NSAClient(sys.argv[1])
    finally:
        c.c.close()
