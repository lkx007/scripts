# 1. 创建一个socket对象
import socket
server = socket.socket()
# 2. 绑定ip和端口
server.bind(('127.0.0.1',9002))
# 3. 监听是否有客户端连接
server.listen()
print("服务端已经启动9002端口.....")
# 4. 接收客户端连接
sockobj , address =server.accept()
while True:
     # 5. 接收客户端发送的消息
    recv_data = sockobj.recv(1024).decode('utf-8')
    print('client>:%s' %(recv_data))
    if recv_data == 'quit':
        break
    # 6. 给客户端回复消息
    send_data = input("server>:")
    sockobj.send(send_data.encode('utf-8'))
    if send_data == 'quit':
        break
# 7. 关闭socket对象
sockobj.close()
server.close()
