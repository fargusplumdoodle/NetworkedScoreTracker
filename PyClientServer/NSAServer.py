import re
from collections import deque
import select
import json
import sys
import socket
import time
import threading

"""
    Network Score Application

    Author: Isaac Thiessen Mar 2019

    Plan:
        1. Make system functional without error handling
        2. Add error handling/player disconnects
        
"""

TIMEOUT = 5

class STATES:
    HOST_GAME = 0
    IN_GAME = 1
    STARTING_GAME = 2


class SockManager(threading.Thread):
    def __init__(self, port=8979):
        super(SockManager, self).__init__()
        self.q = deque()
        self.active_conns = set()
        self.num_clients_served = 0
        self.port = port
        self.max_q_size = 3

        self.alternate_port_modifier = 1

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

        s.settimeout(TIMEOUT)

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
                    c.settimeout(TIMEOUT)
                    c.connect(('localhost', self.port))
                    c.send("DONE".encode('utf-8'))
                    # this works because this loop will stop running once the states have changed
                continue

            else:
                client_info = self.q.pop()

                ch = ClientHandler(client_info[0], client_info[1], (self.port + self.alternate_port_modifier))

                # incramenting alternate port modifier to ensure all secondary lines are running on different ports
                self.alternate_port_modifier += 1

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
        # TODO: FIX THIS! A SINGLE CLIENT WILL STOP THE WHOLE SERVER IF SOMETHING IS WRONG

        for player in self.client_players:
            player.start_game()

        self.state = STATES.IN_GAME

        for player in self.client_players:
            player.start()

        self.life_update()

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

    def check_for_new_life(self):
        # here we check for each players updated life
        for player in self.client_players:

            # new_life will be none if the user has not set their life
            if player.new_life is not None:

                # setting the players life
                self.player_lives[player.client_name] = player.new_life

                # resetting the new life to be None
                player.new_life = None

                self.life_update()

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
                self.check_for_new_life()
                time.sleep(1)


class ClientHandler(threading.Thread):
    """
    ClientHandler class for Network Score App

    Each instance of this object is for communicating with a single client
    2 lines of communication:
        1. For sending life updates to client including the life total of each player
            in the form of a json string
        2. For receiving new life values from client

    """

    def __init__(self, client_soc, client_addr, secondary_port):

        # TODO: BIG TODO:
        #   ERROR HANDLING

        super(ClientHandler, self).__init__()
        self.secondary_port = secondary_port
        self.c = client_soc
        self.client_addr = client_addr
        self.client_name = ''
        self.packet_len = 1024

        self.rec_life_soc = None

        self.new_life = None

    def run(self):
        while True:
            self.receive_life()

    def establish_secondary_communication(self):
        print("started second line protocol")
        """
        Here we create a new socket for recieving life from the client so
        we dont get messages from the wrong protocol

        Protocol:
            1. server connects to client
            2. client says "NEW_LINE"
            3. server says "BOOYAH"
        """
        # sleeping for 1 second to give client time to set up server
        time.sleep(1)
        # 0. creating socket
        self.rec_life_soc = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.rec_life_soc.settimeout(TIMEOUT)

        # 1.
        print("connecting to secondary line on port %s" % self.secondary_port)
        self.rec_life_soc.connect((self.client_addr[0], self.secondary_port))

        # 2.
        response = self.rec_life_soc.recv(self.packet_len).decode('utf-8')
        if response != "NEW_LINE":
            raise ValueError('Error: Invalid response from server, expecting %s got %s' % ("NEW_LINE", response))

        # 3.
        self.rec_life_soc.send("BOOYAH".encode("utf-8"))
        print("Second line established with client")

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
        self.client_name = self.recv()

        print('Client name ', self.client_name)

        # 5. server says OK
        self.send('OK')

        # When the user running the server starts the game, we will move to start game

    def recv(self):
        return self.c.recv(self.packet_len).decode('utf-8')

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
        # 1.
        self.send("START_GAME")

        # 2.
        self.recv_expect("OK")

        # 3.
        self.send(str(self.secondary_port))

        # 4.
        self.establish_secondary_communication()

        # 5/6 handled by parent function

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

    def receive_life(self):
        """
        Protocol:
            1. listen for new life from client
            2. client says NEW_LIFE
            3. server says READY
            4. client sends life as a positive integer
            5. server sends new life to each player
        """

        # 2.
        self.recv_expect_line2("NEW_LIFE")

        # 3.
        self.send_line2("READY")

        # 4.
        new_life = self.recv_line2()
        print("new life from %s: %s" % (self.client_name, self.new_life))
        self.print_proto("receive_life", 4)

        # 5.
        self.send_line2("OK")

        self.new_life = new_life

    def send(self, msg):
        self.c.send(msg.encode('utf-8'))

    def recv_expect(self, expected_msg):
        response = self.c.recv(self.packet_len).decode('utf-8')
        if response != expected_msg:
            raise ValueError('Error: Invalid response from client, expecting %s got %s' % (expected_msg, response))

    def send_line2(self, msg):
        self.rec_life_soc.send(msg.encode('utf-8'))

    def recv_expect_line2(self, expected_msg):
        response = self.rec_life_soc.recv(self.packet_len).decode('utf-8')
        if response != expected_msg:
            raise ValueError('Error: Invalid response from client, expecting %s got %s' % (expected_msg, response))

    def recv_line2(self):
        return self.rec_life_soc.recv(self.packet_len).decode('utf-8')

    def print_proto(self, protocol, step):
        print("Protocol: %s %s" % (protocol, step))

    def disconnect(self):
        x = input("Disconnect? [y/N]: \m")
        if x == "y":
            exit(-3)

if __name__ == '__main__':
    man = SockManager(int(sys.argv[1]))
    try:
        man.start()
        man.pre_game_handle_clients()
    finally:
        man.s.close()

