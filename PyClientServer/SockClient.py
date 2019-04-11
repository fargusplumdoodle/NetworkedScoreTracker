#!/usr/bin/python3

# Client script for lab3 ICS 226
import socket
import os


class SockClient(object):

    def __init__(self, command, target, proto='tcp', port=12345, packet_len=1024, server_ip='localhost'):

        self.command = command
        self.target = target
        self.server_addr = (server_ip, port)
        self.c = None
        self.packet_len = packet_len


        if not os.access(self.target, os.R_OK) and command == 'PUT':
            error = 'Error Client unable to access file: %s' % self.target
            #print(error)
            exit(-1)

        self.error_codes = {
            1: 'Server unable to access file',
            2: 'Server unable to delete file',
            3: 'Server unable to create file'
        }

        if proto == 'tcp':
            self.init_tcp()


        #print('Waiting for ready signal')
        response = self.c.recv(self.packet_len).decode('utf-8')
        if response != 'READY':
            #print('Response: %s' % response)
            raise ValueError('Error: Invalid response from server, expecting READY.\n Command: %s, %s' % (self.command, self.target))
            exit(1)

        # Server is ready for name

        # sending name
        print(response)
        self.send(command)
        response = self.c.recv(self.packet_len).decode('utf-8')
        print(response)
        exit(-2)


        if self.command == 'GET':
            self.get()
        elif self.command == 'PUT':
            self.put()
        elif self.command == 'DEL':
            self.delt()

    def delt(self):
        #print ('Waiting for OK signal')
        response = self.c.recv(self.packet_len).decode('utf-8')
        # This is were a potential error may arise
        self.check_error(response)
        print ('client deleting file ' + self.target)
        print ('Complete')

    def get(self):
        #print ('Waiting for OK signal')
        response = self.c.recv(self.packet_len).decode('utf-8')
        # This is were a potential error may arise
        self.check_error(response)

        #print ('Received OK signal, sending READY signal')
        self.send('READY')

        #print ('Waiting for num of bytes')
        response = self.c.recv(self.packet_len)

        expected_bytes = int.from_bytes(response, byteorder='big')
        #print ('num bytes: %s' % (expected_bytes))

        #print ('Sending OK signal')
        self.send('OK')

        # if the file exists
        if os.path.exists(self.target):
            # and we dont have write access
            if not os.access(self.target, os.W_OK):
                # that is an issue
                raise OSError('Error: Client unable to write to:', self.target)

        #print ('client receiving file %s (%s bytes)' % (self.target, expected_bytes))
        fl = open(self.target, 'wb')

        # Receiving bytes
        bytes_received = 0
        packets_received = 0

        try:
            while bytes_received <= expected_bytes:

                data = self.c.recv(self.packet_len)

                if data[-4:] == b'DONE':
                    packets_received += 1
                    fl.write(data[:-4])
                    break

                if len(data) == 0 or data == b'':
                    break

                fl.write(data)
                packets_received += 1

        finally:
            fl.close()

        print('Complete')
        exit()

    def check_error(self, msg):
        if msg[:5] == 'ERROR':
            print(msg)
            exit(-1)

    def put(self):

        self.recv_ok()


        #print ('Sending num of bytes:', os.path.getsize(self.target))
        #print ('')
        num_bytes = os.path.getsize(self.target).to_bytes(8, byteorder='big')
        self.c.send(num_bytes)

        self.recv_ok()

        # Sending file to Server
        #print('Sending file')
        import time
        start = time.time()

        print('client sending file ' + self.target + ' (' + str(os.path.getsize(self.target)) + ' bytes)')
        self.send_file()

        duration = time.time() - start
        print('Complete')

    def recv_ok(self):
        #print ('Waiting for OK signal')
        response = self.c.recv(self.packet_len).decode('utf-8')
        # This is were a potential error may arise
        self.check_error(response)

        if response != 'OK':
            #print('Response: %s' % response)
            raise ValueError('Error: Invalid response, expecting OK signal')
            exit(1)

    def send(self, msg):
        msg = msg.encode('utf-8')
        self.c.send(msg)

    def init_tcp(self):
        self.c = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.c.connect(self.server_addr)

    def get_data(self):
        '''Receives data, in chunks of the specified length
            Then concatenates it into a string and decodes it from utf-8'''
        data = []
        while True:
            chunk = self.c.recv(self.packet_len)
            if chunk == '' or chunk == ''.encode('utf-8'):
                return data
            else:
                data.append(chunk.decode('utf-8'))
        return ''.join(data).decode('utf-8')

    def send_file_old(self):
        '''This will open a file and put it into a list
           each element will be a string with the maximum size
           that was specified in the variable self.packet_len'''
        if not os.access(self.target, os.R_OK):
            raise OSError('Unable to access file: %s' % self.target)

        fl = open(self.target, 'rb')

        chunk = ''
        count = 0
        packet_no = 0

        for line in fl:
            for x in line:
                if count == self.packet_len:
                    # Here we would send the chunk, but for now we will #print it
                    packet_no += 1
                    self.send(chunk)
                    chunk = ''
                    count = 0

                chunk += str(x)
                count += 2

        # Sending the remaining bits
        self.send(chunk)
        fl.close()

    def send_file(self):
        '''This will open a file and put it into a list
           each element will be a string with the maximum size
           that was specified in the variable self.packet_len'''
        if not os.access(self.target, os.R_OK):
            raise OSError('Unable to access file: %s' % self.target)

        with open(self.target, 'rb') as f:
            pack = f.read(self.packet_len)
            while pack:
                self.c.send(pack)
                pack = f.read(self.packet_len)
        self.send('DONE')

if __name__ == '__main__':
    import sys

    try:
        c = SockClient(server_ip='localhost', port=int(sys.argv[1]), command='fargus', target='localhost')

    except IndexError as e:
        print('SockClient Python3 Nov15 2018')
        print()
        print('Error:' + str(e))
        print()
        print('Syntax: \n'
              '     Port  1\n'
              '     Command 2\n'
              '     Target File 3\n')
        print()
        exit(2)

































