import re
from collections import deque
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

TIMEOUT = 200


class STATES:
    HOST_GAME = 0
    IN_GAME = 1
    STARTING_GAME = 2


def get_tcp_socket(port):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    # Setting address info
    bind_address = ('0.0.0.0', port)

    # Binding to address
    s.bind(bind_address)
    s.listen(5)  # No more than 5 connections
    return s


class SockManager(threading.Thread):
    def __init__(self, server_socket, port=8979):
        super(SockManager, self).__init__()
        self.q = deque()
        self.num_clients_served = 0
        self.port = port
        self.max_q_size = 3
        self.alternate_port_modifier = 1
        self.max_players = 1

        self.initial_life = 20

        self.s = server_socket

        # initializing as hosting game
        self.state = STATES.HOST_GAME

        # players in format:
        # 'fargus': ('192.168.0.5', 20, socket_object)
        #  name         ip address  life
        self.client_players = set()

        self.player_lives = {}

        # checking for disconnects
        self.disconnect_checking_thread = threading.Thread(target=self.check_for_disconnects)
        self.disconnect_checking_thread.start()

    def run_server(self):
        self.start()
        self.pre_game_handle_clients()


    def pre_game_handle_clients(self):
        while self.state == STATES.HOST_GAME:
            # First check if any active connections
            if (len(self.q)) == 0:
                # NO active connections, wait a sec and try again
                if len(self.client_players) >= self.max_players:
                    print("Starting the game because the maximum number of players have joined")
                    self.state = STATES.STARTING_GAME

                    # this is to stop listening for new clients        self.max_players = 1
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
            if not player.disconnected:
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

    def kick_player(self, client):
        """
        For removing disconnected clients, or just people who are jerks
        :param client: ClientHandler object
        """
        print("KICKING PLAYER: ", client.client_name)
        # deleting from life totals
        self.player_lives[client.client_name] = "DISCONNECTED"

        # removing from active connections
        self.client_players.remove(client)

        # ending thread
        client.stop()

        # informing the players, they will be disappointed
        self.life_update()

    def check_for_disconnects(self):
        print("Checking for disconnects start")
        while True:
            # removing disconnected clients
            kick = []
            for client in self.client_players:
                if client.disconnected:
                    print("FOUND DISCONNECTED CLIENT")
                    kick.append(client)

            for dead_client in kick:
                self.kick_player(dead_client)

            time.sleep(0.3)

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

        self.c.settimeout(TIMEOUT)

        self.client_addr = client_addr
        self.client_name = ''
        self.packet_len = 1024

        self.rec_life_soc = None

        self.disconnected = False

        self.new_life = None

    def run(self):
        while True:
            if self.disconnected:
                self.stop()
            self.receive_life()

    def establish_secondary_communication(self):
        print("started second line protocol")
        """
        Here we create a new socket for recieving life from the client so
        we dont get messages from the wrong protocol

        Protocol:
            1. client connects to server on secondary port
            2. server says "NEW_LINE"
            3. client says "BOOYAH"
        """
        # sleeping for 1 second to give client time to set up server
        time.sleep(1)
        # 0. creating socket
        tmp_soc = get_tcp_socket(self.secondary_port)

        # 1.
        print('waiting for client on second line')
        self.rec_life_soc, addr = tmp_soc.accept()

        # 2.
        print('waiting for client on second line')
        # self.recv_expect_line2("NEW_LINE")
        self.send_line2('NEW_LINE')

        # 3.
        # self.send_line2("BOOYAH")

        self.recv_expect_line2("BOOYAH")
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
        try:
            msg = self.c.recv(self.packet_len).decode('utf-8')
            # removing non alphanumeric characters because java sends 2 wierd characters at the begginging of string
            msg = re.sub(r'\W+', '', msg)
        except socket.timeout:
            self.disconnected = True
            print("Disconnecting", self.client_name, "timeout")
            time.sleep(10)
        return msg

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
        print('Starting START_GAME protocol for client %s' % self.client_name)
        # 1.
        self.send("START_GAME")

        # 2.
        print('receiving Ok at START_GAME/2')
        self.recv_expect("OK")

        # 3.
        print('Sending secondary port %s at START_GAME/3' % self.secondary_port)
        self.send(str(self.secondary_port))

        # 4.
        print('Establishing second line of communication at START_GAME/4')
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
        print("new life from %s: %s" % (self.client_name, new_life))

        # 5.
        self.send_line2("OK")

        self.new_life = new_life

    def send(self, msg):
        if self.disconnected:
            # we dont send things when we are disconnected
            print("NOT SENDING MSG BECAUSE DISCONNECTED")
            time.sleep(10)
        self.c.send(msg.encode('utf-8'))

    def recv_expect(self, expected_msg):
        try:
            response = self.c.recv(self.packet_len).decode('utf-8')

            # removing non alphanumeric characters because java sends 2 wierd characters at the begginging of string
            response = re.sub(r'\W+', '', response)

        except socket.timeout:
            self.disconnected = True
            print("Disconnecting", self.client_name, "timeout")
            response = "OH NOOOO"

        if response != expected_msg:
            print('Error: Invalid response from client, expecting %s got %s' % (expected_msg, response))
            self.disconnected = True

    def send_line2(self, msg):
        if self.disconnected:
            # we dont send things when we are disconnected
            print("NOT SENDING MSG BECAUSE DISCONNECTED")
            time.sleep(10)
        self.rec_life_soc.send(msg.encode('utf-8'))

    def recv_expect_line2(self, expected_msg):
        response = self.rec_life_soc.recv(self.packet_len).decode('utf-8')
        # removing non alphanumeric characters because java sends 2 wierd characters at the begginging of string
        response = re.sub(r'\W+', '', response)
        if response != expected_msg:
            print('Error: Invalid response from client, expecting %s got %s' % (expected_msg, response))
            self.disconnected = True

    def recv_line2(self):
        response = self.rec_life_soc.recv(self.packet_len).decode('utf-8')

        # removing non alphanumeric characters because java sends 2 wierd characters at the begginging of string
        response = re.sub(r'\W+', '', response)

        return response

    def stop(self):
        self.c.close()
        exit(0)

    def disconnect(self):
        x = input("Disconnect? [y/N]: \m")
        if x == "y":
            self.disconnected = True


if __name__ == '__main__':
    soc = get_tcp_socket(port=int(sys.argv[1]))
    try:
        man = SockManager(soc, port=int(sys.argv[1]))
        man.run_server()
    finally:
        soc.close()

