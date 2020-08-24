# -*- coding:utf-8 -*-
import socket
import socketserver
import threading
import pandas as pd

PORT = 11121
ip = socket.gethostbyname(socket.gethostname())
ip = "192.168.1.101"
print('[     ip     ]:', ip)

col_names = ['session', 'block', 'trial', 'func_selected', 'func_target', 'configure', 'timestamp_pressed', 'timestamp_selected', 'func_id', 'timestamp_func_start']
df = pd.DataFrame(columns=col_names)


user = 'ztx'
age = '31'
filename = "./study1_data/"+user+'_'+age+'.csv'

'''
# socketserver
class MyServer(socketserver.BaseRequestHandler):
    def handle(self):
        client = self.request
        print('new connection: ', self.client_address)
        client.sendall(bytes('你好\n', encoding='utf-8'))
        while True:
            buf_bytes = client.recv(1024)
            print(len(buf_bytes))
            print(buf)
            buf = str(buf_bytes, encoding='utf-8')
            if buf == 'q':
                break
server = socketserver.ThreadingTCPServer((ip, PORT), MyServer)
server.serve_forever()
'''

# 
server = socket.socket()
server.bind((ip, PORT))
server.listen(5)
clients = []

def send(client, s):
	client.sendall(bytes(s + '\n', encoding='utf-8'))

def recv(client, address, s):
	global df
	print('[%s:%d]: %s' % (address[0], address[1], s))
	if len(s.split(";")) == len(col_names):
		#print(s.split(";"))
		df.loc[len(df)] = s.split(";")
		#row = pd.DataFrame(s.split(";"), columns=col_names, ignore_index=True)
		#df = df.append(row, ignore_index=True)
	elif s == "Experiment Finished":
		df.to_csv(filename)

def recv_thread(client, address):
	while True:
		b = client.recv(1024)
		if len(b) <= 0:
			clients.remove(client)
			print('[disconnected]: %s:%d (%d)' % (address[0], address[1], len(clients)))
			break
		s = str(b, encoding='utf-8')
		recv(client, address, s)

def listen_thread():
	global clients
	print('[ listening  ] ...')
	while True:
		client, address = server.accept()
		clients.append(client)
		print('[ connected  ]: %s:%d (%d)' % (address[0], address[1], len(clients)))
		t = threading.Thread(target=recv_thread, args=[client, address])
		t.start()
		send(client, "服务器端发送!")

t = threading.Thread(target=listen_thread)
t.setDaemon(True)
t.start()

try:
	while True:
		s = input()
		print('[ broadcast  ] %d clients: "%s"' % (len(clients), s))
	#for client in clients:
	#	send(client, s)
except KeyboardInterrupt:
	df.to_csv(filename)