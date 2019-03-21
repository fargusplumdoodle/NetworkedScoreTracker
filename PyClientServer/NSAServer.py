import select
import sys
from collections import deque
import socket
import time
import threading


class STATES:
    HOST_GAME = 0
    IN_GAME = 1
    STARTING_GAME = 2


class SockManager(threading.Thread):
    def __init__(self):
        super(SockManager, self).__init__()
        self.q = deque()
        self.active_conns = set()
        self.num_clients_served = 0
        self.port = 7989
        self.max_q_size = 3

        self.s = self.get_tcp_socket()

        # initializing as hosting game
        self.state = STATES.HOST_GAME

        # players in format:
        # 'fargus': ('192.168.0.5', 20, socket_object)
        #  name         ip address  life
        self.players = {}

    def get_tcp_socket(self):
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

        # Setting address info
        bind_address = ('0.0.0.0', self.port)

        # Binding to address
        s.bind(bind_address)
        s.listen(5)  # No more than 5 connections
        return s

    def pre_game_handle_clients(self):
        while self.state == STATES.HOST_GAME:
            # First check if any active connections
            if (len(self.q)) == 0:
                # NO active connections, wait a sec and try again
                print("Queue empty, you have 3 seconds to start the game...")
                i, o, e = select.select([sys.stdin], [], [], 3)
                if i:
                    print("user has instructed to start the game")
                    self.state = STATES.STARTING_GAME

                    # this is to stop listening for new clients
                    c = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                    c.connect(('localhost', self.port))
                    c.send("DONE".encode('utf-8'))

                    # this works because this loop will stop running once the states have changed
                continue
            else:
                # initiating start_game protocol with client
                client_info = self.q.pop()
                ch = ClientHandler(client_info[0], client_info[1])
                ch.host_game()
                self.players[ch.client_name] = ch

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

    def run(self):
        print("Starting server on port %s" % self.port)
        while True:
            if self.state == STATES.HOST_GAME:
                # getting new client and adding to  queue
                client_soc, client_addr = self.s.accept()
                self.q.append((client_soc, client_addr))
                print('Got client: ', client_addr)

            elif self.state == STATES.STARTING_GAME:
                print('STARTING GAME NOW')
                print('Players:', self.players)

                break


class ClientHandler(threading.Thread):
    def __init__(self, client_soc, client_addr):
        super(ClientHandler, self).__init__()

        self.port = 8989
        self.c = client_soc
        self.client_addr = client_addr
        self.client_name = ''
        self.packet_len = 1024

    def run(self):
        pass

    def host_game(self):
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
        # 3. server says READY
        print('Sending ready')
        self.c.send('READY'.encode('utf-8'))

        # 4. clients send the name of the player ( e.g: "Fargus" )
        # TODO: Error handling
        self.client_name = self.c.recv(self.packet_len).decode('utf-8')

        print('Client name ', self.client_name)

        # 5. server says OK
        self.c.send('OK'.encode('utf-8'))

        # When the user running the server starts the game, we will move to start game

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

    def recieve_life(self):
        pass

    def recv_expect(self, expected_msg):
        response = self.c.recv(self.packet_len).decode('utf-8')
        if response != expected_msg:
            raise ValueError('Error: Invalid response from client, expecting %s got %s' % (expected_msg, response))



if __name__ == '__main__':
    man = SockManager()
    try:
        man.start()
        man.pre_game_handle_clients()
    finally:
        man.s.close()

