# -*- coding:utf-8 -*-
import socket
import socketserver
import threading
import pandas as pd
import datetime

PORT = 11121
# ip = socket.gethostbyname(socket.gethostname())
ip = input("Input IP: ")
print('[     ip     ]:', ip)

col_names = ['session', 'block', 'trial', 'func_selected', 'func_target', 'configure', 'timestamp_pressed', 'timestamp_selected', 'func_id', 'timestamp_func_start','overall', 'mental','physical']
df = pd.DataFrame(columns=col_names)


user = input("Input User: ")
age = '31'
filename = "./study1_data/"+user+'_'+age

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
server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.bind((ip, PORT))
server.listen(5)
clients = []

def rating_input(session, block, trial):
	rating_raw = input("Overal, Mental, Physical Rating for Session"+session+" Block"+block+" Trial"+trial+" :")
	n = 10
	while n > 0:
		try:
			rating = rating_raw.split(' ')
			if len(rating) == 3:
				return rating
			else:
				raise Exception("Input Error!")
		except:
			rating_raw = input("Reenter rating:")
		n = n - 1

def send(client, s):
	client.sendall(bytes(s + '\n', encoding='utf-8'))

def recv(client, address, s):
	global df
	print('[%s:%d]: %s' % (address[0], address[1], s))
	if len(s.split(";")) == len(col_names) - 3:
		#print(s.split(";"))
		s_array = s.split(";")
		session = s_array[0]
		block = s_array[1]
		trial = s_array[2]
		df.loc[len(df)] = s_array + rating_input(session, block, trial)
		print("Done")
		#row = pd.DataFrame(s.split(";"), columns=col_names, ignore_index=True)
		#df = df.append(row, ignore_index=True)
	elif s == "Experiment Finished":
		df.to_csv(filename+'.csv')

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
	if len(clients) == 0:
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
		x = 1
		#s = input()
		#print('[ broadcast  ] %d clients: "%s"' % (len(clients), s))
	#for client in clients:
	#	send(client, s)
except:
	dt = str(datetime.datetime.now()).split(' ')
	df.to_csv(filename+'_'+dt[0]+'_'+dt[1].replace(':','_')+".csv")