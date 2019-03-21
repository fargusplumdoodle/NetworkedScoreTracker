import socket


class NSAServer(object):
    def __init__(self):
        self.port = 8989

        # players in format:
        # 'fargus': ('192.168.0.5', 20)
        #  name         ip address  life
        self.players = {}

    def host_game(self):
        pass

    def start_game(self):
        pass

    def life_update(self):
        pass

    def recieve_life(self):
        pass

