def handle_request(sockobj):
    while True:
        recv_data = sockobj.recv(1024).decode('utf-8')
        print("client>:%s" %(recv_data))
        if recv_data == 'quit':
            break
        send_data = input("server>:")
        sockobj.send(send_data.encode('utf-8'))
        if send_data == 'quit':
            break

from gevent import monkey
monkey.patch_all()
import gevent
import socket

server = socket.socket()
server.bind(('127.0.0.1',9001))
server.listen()
print("服务已经启动9001端口...")
while True:
    sockobj , address = server.accept()
    #创建协程
    gevent.spawn(handle_request,sockobj)
sockobj.close()
server.close()
