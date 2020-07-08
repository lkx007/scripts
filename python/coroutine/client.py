import socket
HOST = '127.0.0.1'
PORT = 9002
# 1. 创建客户端的socket对象
client = socket.socket()
# 2. 连接服务端， 需要指定端口和IP
client.connect((HOST,PORT))
while True:
# 3. 给服务端发送数据
    send_data = input("client>:")
    client.send(send_data.encode('utf-8'))
    if send_data == 'quit':
        break
     # 4. 获取服务端返回的消息
    recv_data = client.recv(1024).decode('utf-8')
    print('server>:%s' %(recv_data))
    if recv_data=='quit':
        break
 # 5. 关闭socket连接
client.close()
