from collections import deque
import socket
import time
import threading


class STATES:
    HOST_GAME = 0
    IN_GAME = 1


class SockManager(threading.Thread):
    def __init__(self):
        print('SockManager works')
        super(SockManager, self).__init__()
        self.q = deque()
        self.active_conns = set()
        self.num_clients_served = 0
        self.port = 8989
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
                time.sleep(2)
                continue
            else:
                # initiating start_game protocol with client
                print('Starting handling client')
                client_info = self.q.pop()
                ch = ClientHandler(client_info[0], client_info[1])
                ch.host_game()
                self.players[ch.client_name] = ch


    def run(self):
        print("Starting server on port %s" % self.port)
        if self.state == STATES.HOST_GAME:
            while True:
                # getting new client and adding to  queue
                client_soc, client_addr = self.s.accept()
                self.q.append((client_soc, client_addr))
                print('Got client')


class ClientHandler(threading.Thread):
    def __init__(self, client_soc, client_addr):
        super(ClientHandler, self).__init__()

        self.port = 8989
        self.c = client_soc
        self.client_addr = client_addr
        self.client_name = ''
        self.packet_len = 1024

    def run(self, state):
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
        pass

    def start_game(self):
        pass

    def life_update(self):
        pass

    def recieve_life(self):
        pass

if __name__ == '__main__':
    man = SockManager()
    man.start()
    man.pre_game_handle_clients()
