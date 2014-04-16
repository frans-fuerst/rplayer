#!/usr/bin/python
import socket
from threading import Thread
import threading 
import time

class serverthread(Thread):
	def __init__ (self,ip):
		Thread.__init__(self)
		self.ip = ip
		self.status = -1
	  
	def run(self):
		#pingaling = os.popen("ping -q -c2 "+self.ip,"r")
		while 1:
			
			time.sleep( 1 )
			print "serverthread: sleeping", threading.currentThread()
			#line = pingaling.readline()
			#if not line: break
			#igot = re.findall(testit.lifeline,line)
			#if igot:
			#   self.status = int(igot[0])

#create an INET, STREAMing socket
serversocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
print serversocket, socket.gethostname()

#bind the socket to a public host, 
# and a well-known port
serversocket.bind((socket.gethostname(), 2000))

#become a server socket
print serversocket.listen(5)

while 1:
	print "Accept", threading.currentThread()
	#accept connections from outside
	(clientsocket, address) = serversocket.accept()
	print "connected: ", address
	#now do something with the clientsocket
	#in this case, we'll pretend this is a threaded server
	ct = serverthread(clientsocket)
	ct.start()

print "Ende"
