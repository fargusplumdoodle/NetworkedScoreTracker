#!/usr/bin/python3
"""

Wrapper for Socket


"""
import socket
import os
import time
import threading
from collections import deque

class SockServer(object):
    def __init__(self, proto='tcp', port=12345, packet_len=1024, interface='0.0.0.0', verbose=False):

        # Initializing Variables
        self.bind_port = port
        self.interface = interface
        self.bind_address = None
        self.s = None
        self.packet_len = packet_len
        self.verbose = verbose

        self.error_codes = {
            1: 'ERROR: %s does not exist',
            2: 'ERROR: unable to delete %s',
            3: 'ERROR: unable to create file %s'
        }

        # Setting state
        self.state = 'WAITING'

        # Using specified protocol
        if proto == 'tcp':
            self.tcp_init()
        else:
            raise ValueError('Only TCP functionality currently exists')

        # Running server
        self.manager = SockManager(self)
        self.manager.start()
        self.runserver()

    def tcp_init(self):
        # Creating server socket object
        self.s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

        # Setting address info
        self.bind_address = (self.interface, self.bind_port)

        # Binding to address
        self.s.bind(self.bind_address)
        self.s.listen(5)  # No more than 5 connections


    def get_data(self, c_sock):
        '''Receives data, in chunks of the specified length
            Then concatenates it into a string and decodes it from utf-8'''
        data = []
        while True:
            chunk = c_sock.recv(self.packet_len)
            if chunk == '' or chunk == ''.encode('utf-8'):
                break
            else:
                data.append(chunk.decode('utf-8'))

        return ''.join(data)

    def delt(self, client_soc, target):
        cont = True
        # Verifying we can access file
        if not os.path.exists(target):
            error = self.error_codes[1] % target
            #print(error)
            client_soc.send(error.encode('utf-8'))
            cont = False

        if cont:
            # Attemping to remove target
            try:
                os.remove(target)
            except OSError:
                error = self.error_codes[2] % target
                #print(error)
                client_soc.send(error.encode('utf-8'))
                cont = False

        if cont:
            self.vprint('server deleting file %s' % target)
            #print ('Sending OK Signal')
            client_soc.send('OK'.encode('utf-8'))

    def get(self, client_soc, target):
        cont = True

        # Verifying we can access file
        if not os.access(target, os.R_OK):
            error = self.error_codes[1] % target
            #print(error)
            client_soc.send(error.encode('utf-8'))
            cont = False

        if cont:
            #print ('Sending OK Signal')
            client_soc.send('OK'.encode('utf-8'))

            #print ('Waiting for READY')
            response = client_soc.recv(self.packet_len).decode('utf-8')
            if response != 'READY':
                raise ValueError('Error: Expected ready, got %s ' % response)

            #print ('Sending num of bytes')
            num_bytes = os.path.getsize(target).to_bytes(8, byteorder='big')
            self.vprint('server sending %d bytes' % os.path.getsize(target))
            client_soc.send(num_bytes)

            #print ('Waiting for OK')
            response = client_soc.recv(self.packet_len).decode('utf-8')
            if response != 'OK':
                raise ValueError('Error: Expected ready, got %s ' % response)

            # Sending file to client
            self.send_file(target, client_soc)

            client_soc.send('DONE'.encode('utf-8'))

    def put(self, client_soc, target):
        #print ('Sending OK Signal')
        client_soc.send('OK'.encode('utf-8'))

        num_bytes = int.from_bytes(client_soc.recv(self.packet_len), byteorder='big')
        self.vprint('server receiving %d bytes' % num_bytes)

        #print ('Number of bytes for file:', num_bytes)

        #print ('Sending OK Signal')
        client_soc.send('OK'.encode('utf-8'))

        self.recv_file(client_soc, target, num_bytes)

    def recv_file(self, client_soc, target, expected_bytes):
        # if the file exists
        if os.path.exists(target) and not os.access(target, os.W_OK):
            raise OSError('Error: Client unable to read from:', self.target)

        fl = open(target, 'wb')

        # Receiving bytes
        bytes_received = 0
        packets_received = 0

        try:
            while bytes_received <= expected_bytes:
                data = client_soc.recv(self.packet_len)
                if data.decode('utf-8')[-4:] == 'DONE':
                    packets_received += 1
                    fl.write(data[:-4])
                    break

                if len(data) == 0 or data == b'':
                    break

                fl.write(data)
                packets_received += 1
                # #print('Recieved Packet:', packets_received)


        finally:
            fl.close()

        #print('Finished writing to file')
        #print('Packets received:', packets_received)

    def send_file(self, target, client_soc):
        '''This will open a file and put it into a list
           each element will be a string with the maximum size
           that was specified in the variable self.packet_len'''
        if not os.access(target, os.R_OK):
            raise OSError('Unable to access file: %s' % self.target)

        with open(target, 'rb') as f:
            pack = f.read(self.packet_len)
            while pack:
                client_soc.send(pack)
                pack = f.read(self.packet_len)

    def send_file_old(self, target, client_soc):
        '''This will open a file and put it into a list
           each element will be a string with the maximum size
           that was specified in the variable self.packet_len'''
        if not os.access(target, os.R_OK):
            raise OSError('Unable to access file: %s' % target)

        file = open(target, 'r')

        chunk = ''
        count = 0
        packet_no = 0

        for line in file:
            for x in line:
                if count == self.packet_len:
                    # Here we would send the chunk, but for now we will #print it
                    packet_no += 1
                    ##print ('Sending chunk number:', packet_no)
                    client_soc.send(chunk.encode('utf-8'))
                    #client_soc.send(chunk)
                    chunk = ''
                    count = 0

                chunk += str(x)
                count += 1

        # Sending the remaining bits
        client_soc.send(chunk.encode('utf-8'))
        file.close()

    def handle_client(self, client_soc, client_addr):
        self.vprint('server connected to client at %s:%s' % client_addr)
        # Sending client ready signal
        client_soc.send('READY'.encode('utf-8'))

        response = client_soc.recv(self.packet_len).decode('utf-8')

        command, target = response.split(' ')[0], response.split(' ')[1]
        self.vprint('server receiving request: %s' % command + ' ' + target)

        if command == 'GET':
            self.get(client_soc, target)
        elif command == 'PUT':
            self.put(client_soc, target)
        elif command == 'DEL':
            self.delt(client_soc, target)
        else:
            print('Error: invalid input from client')

    def runserver(self):
        while True:
            self.vprint('server waiting on port %d' % self.bind_port)
            client_soc, client_addr = self.s.accept()
            self.manager.add_client(client_soc, client_addr)
            #self.handle_client(client_soc, client_addr)

    def vprint(self, msg):
        if self.verbose:
            print(msg)



class SockManager(threading.Thread):

    def __init__(self, server):
        print('SockManager works')
        super(SockManager, self).__init__()
        self.q = deque()
        self.active_conns = set()
        self.server = server
        self.num_clients_served = 0
        self.max_q_size = 3

    def add_client(self, client_soc, client_addr):
        conn = HandleClient(self.server, client_soc, client_addr)
        self.q.append(conn)
        #print('Queue length: %s, active connections: %s, served: %s' % (len(self.q), len(self.active_conns), self.num_clients_served))

    def run(self):
        while True:
            #import pdb; pdb.set_trace()
            # First check if any active connections
            if (len(self.q)) == 0:
                # NO active connections, wait a sec and try again
                time.sleep(2)
                continue

            # process the next thing
            if (len(self.active_conns) < self.max_q_size):
                # removing from waiting que
                conn = self.q.popleft()
                # starting handling of client
                conn.start()
                # add thread to active conections
                self.active_conns.add(conn)
                self.num_clients_served += 1

            # killing dead processes
            kicklist = []
            for conn in self.active_conns:
                if not conn.isAlive():
                    kicklist.append(conn)
            for conn in kicklist:
                self.active_conns.remove(conn)

            # if self.num_clients_served > 9:
            #     print('length of que %s, clients served %s' % (self.num_clients_served,  len(self.q)))


class HandleClient(threading.Thread):
    def __init__(self, server, client_soc, client_addr):
        super(HandleClient, self).__init__()
        self.server = server
        self.client_soc = client_soc
        self.client_addr = client_addr
        self.started = False

    def run (self):
        self.started = True
        self.server.handle_client(self.client_soc, self.client_addr)


if __name__ == '__main__':
    import sys

    verbose = False

    if len(sys.argv) > 1:
        if '-v' in sys.argv:
            verbose = True

    try:
        server = SockServer(port=int(sys.argv[1]), verbose=verbose)
    except IndexError as e:
        print()
        print('SockClient Python3 Nov15 2018')
        print()
        print('Error:' + str(e))
        print()
        print('Syntax: \n'
              '     port=$1 \n'
              '     -v=optional')
        print()
        exit(1)


