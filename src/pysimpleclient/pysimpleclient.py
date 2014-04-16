#!/usr/bin/python
import socket
from threading import Thread
import time
import sys

class receiveThread(Thread):
	def __init__( self, a_socket ):
		Thread.__init__(self)
		self.m_socket = a_socket
		
	def run( self ):
		lasttime = time.time()
		while True:
			chunk = self.m_socket.recv(100)
			if not chunk: break
#				raise RuntimeError, "socket connection broken"
			acttime = time.time()
			print int((acttime-lasttime) * 1000), "server sent: " + chunk
			lasttime = acttime
			
			if chunk == 'quit\n':
				print "Exit program!"
				quit()
			
print "simple rplayer client for prototyping purposes ver", time.time()

#l_ip_address = "10.0.0.39"
#l_ip_address = "127.0.0.1"
l_ip_address = "192.168.1.4"
#l_ip_address = "10.0.0.31"
l_port = 2012
	
print "create socket.."
#create an INET, STREAMing socket
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

print "connect to IP address '%s' '%d'" % (l_ip_address, l_port)
s.connect((l_ip_address, l_port))

print "connected!"

t = receiveThread( s )
t.start()

s.send( "basedir /media/2f79a798-ca3f-46dd-9c72-132680f68cf0/_AUDIO\n" )

s.send( "list .\n" )

while True:
    print "enter a command"
    a = raw_input()
    if( a == "exit" ): 
        print "close program"
        t.join()
        s.shutdown()
        quit()
    print( "sending '" + a + "'" )
    s.send( a + "\n" )

