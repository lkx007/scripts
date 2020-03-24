systemctl restart nfs-server.service
systemctl restart nfs.service
exportfs -r
showmount -e
