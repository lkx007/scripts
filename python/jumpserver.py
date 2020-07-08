#! /usr/bin/env python
# -*- coding: utf-8 -*-

'''

 回话跟踪，会话管理，流量回放，脚本透传

 第一个就是可以看到跳板机上的用户会话的管理，
 第二个可以对用户通过跳板机做目标机器上做的操作可以回放出来。
 第三个是可以把我们的worker调用通过跳板机的方式到目标机器进行相应的脚本之行，指标采集，文件分发等等。
 会话管理比较宽泛，你可以展开一下，比如可以直接中断某个会话。



认证方式：
使用账号密码调用 Api获取token -H "Authorization: Bearer 2d49c4cf353440ff9f80c21910e7a19e"
用户的private_token -H "Authorization: Token f8a8e2462694b49faf906ec9929725be70cb2404"
private token获取方法：
    source /opt/py3/bin/activate
    cd /opt/jumpserver/apps
python manage.py shell << EOF
from users.models import User
u = User.objects.get(username='admin')
u.create_private_token()
EOF
  如果生成报错, 表示已经存在 private_token, 直接获取即可

u.private_token

'''

import requests
import json


def get_token():
    url = 'http://192.168.2.81/api/authentication/v1/auth/'
    query_args = {
        "username": "admin",
        "password": "admin"
    }
    response = requests.post(url, data=query_args)
    return json.loads(response.text)['token']


def get_user_info():
    '''
curl http://192.168.2.81/api/users/v1/users/ -H "Authorization: Bearer 2d49c4cf353440ff9f80c21910e7a19e"
    :return:
[
    {
        "avatar_url": "/static/img/avatar/admin.png",
        "comment": "",
        "created_by": "",
        "date_expired": "2090-05-31 10:30:30 +0800",
        "date_password_last_updated": "2020-06-17 10:30:36 +0800",
        "email": "admin@mycomany.com",
        "groups": [
            "38c6e906-b4a9-417b-9c6f-f43b3902a5de"
        ],
        "groups_display": "Default",
        "id": "9e3f6d7e-f691-4fb9-b006-a7946a9edd34",
        "is_active": true,
        "is_expired": false,
        "is_first_login": false,
        "is_valid": true,
        "name": "Administrator",
        "otp_level": 0,
        "phone": null,
        "role": "Admin",
        "role_display": "\u7ba1\u7406\u5458",
        "source": "local",
        "source_display": "Local",
        "username": "admin",
        "wechat": ""
    }
]
    '''
    url = 'http://192.168.2.81/api/users/v1/users/'
    token = get_token()
    print(token)
    header_info = {"Authorization": 'Bearer ' + token}
    response = requests.get(url, headers=header_info)
    print(json.loads(response.text))


def get_terminal():
    '''
curl http://192.168.2.81/api/terminal/v1/terminal/  -H "Authorization: Bearer 2d49c4cf353440ff9f80c21910e7a19e"
    :return:
[
    {
        "comment": "From Guacamole Jumpserver Extension",
        "http_port": 5000,
        "id": "14722d14-7d7d-4132-b345-ca3cfb044882",
        "is_accepted": true,
        "is_active": true,
        "is_alive": true,
        "name": "[Gua]jms_all",
        "remote_addr": "127.0.0.1",
        "session_online": 0,
        "ssh_port": 2222
    },
    {
        "comment": "Coco",
        "http_port": 5000,
        "id": "3979c910-5817-4570-9115-6e3a5fdf7684",
        "is_accepted": true,
        "is_active": true,
        "is_alive": true,
        "name": "jms_all",
        "remote_addr": "127.0.0.1",
        "session_online": 0,
        "ssh_port": 2222
    }
]
    '''
    url = 'http://192.168.2.81/api/terminal/v1/terminal/'
    token = get_token()
    print(token)
    header_info = {"Authorization": 'Bearer ' + token}
    response = requests.get(url, headers=header_info)
    print(json.loads(response.text))


def get_terminal_session():
    '''
curl http://192.168.2.81/api/terminal/v1/terminal/sessions/  -H "Authorization: Bearer 2d49c4cf353440ff9f80c21910e7a19e"
    :return:
[
    {
        "asset": "192.168.2.77",
        "can_replay": true,
        "command_amount": 0,
        "date_end": "2020-06-18 10:26:30 +0800",
        "date_start": "2020-06-18 10:05:02 +0800",
        "has_replay": true,
        "id": "f7e623b1-fd30-4737-9918-53b83b65ee71",
        "is_finished": true,
        "login_from": "WT",
        "login_from_display": "Web Terminal",
        "org_id": "",
        "org_name": "DEFAULT",
        "protocol": "rdp",
        "remote_addr": "10.10.101.101",
        "system_user": "Administrator",
        "terminal": "14722d14-7d7d-4132-b345-ca3cfb044882",
        "user": "Administrator (admin)"
    },....
]
    '''
    url = 'http://192.168.2.81/api/terminal/v1/terminal/{}/sessions/'.format("3979c910-5817-4570-9115-6e3a5fdf7684")
    token = get_token()
    print(token)
    header_info = {"Authorization": 'Bearer ' + token}
    response = requests.get(url, headers=header_info)
    print(json.loads(response.text))


def get_session_replay():
    '''
curl http://192.168.2.81/api/terminal/v1/sessions/9c2b490f-ee5c-4e26-91ba-c3f6d11da101/replay/  -H "Authorization: Bearer 2d49c4cf353440ff9f80c21910e7a19e"
    :return:
{"type":"guacamole","src":"/media/replay/2020-06-17/9c2b490f-ee5c-4e26-91ba-c3f6d11da101.replay.gz"}

wget http://192.168.2.81/media/replay/2020-06-17/9c2b490f-ee5c-4e26-91ba-c3f6d11da101.replay.gz
    '''
    url = 'http://192.168.2.81/api/terminal/v1/sessions/{}/replay/'.format("3979c910-5817-4570-9115-6e3a5fdf7684")
    header_info = {"Authorization": 'Token ' + "f8a8e2462694b49faf906ec9929725be70cb2404"}
    response = requests.get(url, headers=header_info)
    print(json.loads(response.text))


def get_web_console():
    '''
curl http://192.168.2.81/terminal/web-terminal   -H "Authorization: Bearer 2d49c4cf353440ff9f80c21910e7a19e" -L
    :return: html文件
<!doctype html><html lang="en"><head><meta charset="utf-8"><title>Luna - Jumpserver web terminal</title><base href="/luna/"><meta name="viewport" content="width=device-width,initial-scale=1"><link rel="icon" type="image/x-icon" href="favicon.ico"><link href="/luna/styles.29c1004c7c2482c9d78e.bundle.css" rel="stylesheet"/></head><body><app-root></app-root><span id="marker" style="display: none;font-size: 14px">marker</span><script type="text/javascript" src="/luna/inline.6602a1f82e32b48c37e4.bundle.js"></script><script type="text/javascript" src="/luna/polyfills.a77ad0f66aa4cf1a6a23.bundle.js"></script><script type="text/javascript" src="/luna/scripts.613dc347e1d955ee7b9e.bundle.js"></script><script type="text/javascript" src="/luna/main.861b00d9dff656a063ad.bundle.js"></script></body><script>window.onload = function (ev) {
    var clipboardData = "";
    if (!document.hasFocus()) {
      return
    }

    if (navigator.clipboard && navigator.clipboard.readText) {
        navigator.clipboard.readText().then(function textRead(text) {
          clipboardData = text;
        });
    }

    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(clipboardData)
    }
}</script></html>
    '''


get_user_info()
# get_terminal_session()
