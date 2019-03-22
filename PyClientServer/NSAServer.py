import select
import json
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
        self.port = 8989
        self.max_q_size = 3

        self.initial_life = 20

        self.s = self.get_tcp_socket()

        # initializing as hosting game
        self.state = STATES.HOST_GAME

        # players in format:
        # 'fargus': ('192.168.0.5', 20, socket_object)
        #  name         ip address  life
        self.client_players = set()

        self.player_lives = {}

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
                client_info = self.q.pop()

                ch = ClientHandler(client_info[0], client_info[1])

                # initiating start_game protocol with client
                ch.host_game()

                # adding player to list of players
                self.client_players.add(ch)

                self.player_lives[ch.client_name] = self.initial_life

    def host_game(self):
        # getting new client and adding to  queue
        client_soc, client_addr = self.s.accept()
        self.q.append((client_soc, client_addr))
        print('Got client: ', client_addr)

    def start_game(self):
        """
        Protocol:
            For each client:
                1. client listens for the START_GAME signal
                2. client says OK
            3. Once each client has responded with OK
            4. Server sends initial life total
        """
        for player in self.client_players:
            player.start_game()

        self.state = STATES.IN_GAME
        self.life_update()
        # TODO: FIX THIS! A SINGLE CLIENT WILL STOP THE WHOLE SERVER IF SOMETHING IS WRONG

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
        for player in self.client_players:
            player.life_update(json.dumps(self.player_lives))

    def run(self):
        print("Starting server on port %s" % self.port)
        while True:

            if self.state == STATES.HOST_GAME:
                self.host_game()

            elif self.state == STATES.STARTING_GAME:
                print('STARTING GAME NOW')
                print('Players connected: %s ' % len(self.client_players))
                self.start_game()

            elif self.state == STATES.IN_GAME:
                print("IN GAME NOW")
                time.sleep(1)
                continue


class ClientHandler(threading.Thread):

    def __init__(self, client_soc, client_addr):

        # TODO: BIG TODO:
        #       Make every method here run on a seperate thread
        #       This will protect us against client disconnects, stopping our server
        #       I suggest we incorporate states and make every function be called from
        #       run
        #       FOR NOW ALL ACTIONS ON ALL CLIENTS WILL BE RAN SEQUENTIALLY AND WE
        #       ASSUME THEY ALL GO SMOOTHLY
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
        self.send('READY')

        # 4. clients send the name of the player ( e.g: "Fargus" )
        # TODO: Error handling/ input validation
        self.client_name = self.c.recv(self.packet_len).decode('utf-8')

        print('Client name ', self.client_name)

        # 5. server says OK
        self.send('OK')

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
        # 1.
        self.send("START_GAME")

        # 2.
        self.recv_expect("OK")

    def life_update(self, game_info):
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
        self.send("LIFE_UPDATE")

        # 3.
        self.recv_expect("OK")

        # 4.
        self.send(game_info)

        # 5.
        self.recv_expect("OK")
        pass

    def receive_life(self):
        pass

    def send(self, msg):
        self.c.send(msg.encode('utf-8'))

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

