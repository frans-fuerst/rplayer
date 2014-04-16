#!/usr/bin/python

import socket
import time

host='127.0.0.1'

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.bind((host,0))
sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1) 

while True:
	print "sending.. ",
	sock.sendto("x", (host,2011))
	print "sent"
	time.sleep( 1 )
	
