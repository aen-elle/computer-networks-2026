import struct
import os
import time 
import socket

def send_pings(num_pings: int, destination: str):
    loss_count = 0
    rtt_lst = []

    for i in range(num_pings):
        resp = send_normal_ping(destination, i+1)
        if resp is None:
            loss_count += 1
        else: 
            rtt_lst.append(resp)
    if (len(rtt_lst) > 0):
        print(f"rtt min/max/avg - {min(rtt_lst)}ms/{max(rtt_lst)}ms/{round(sum(rtt_lst)/len(rtt_lst), 2)}ms")
        print(f"Package loss is {(loss_count / num_pings)*100}%")
    else:
        print("None of the packets received")
        print("Package loss is 100.0%")


def send_normal_ping(destination: str, seq_num: int):
    sock = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_ICMP)
    sock.settimeout(1)
    proc_id = os.getpid() & 0xFFFF
    packet = make_echo_req(seq_num, proc_id)

    try:
        sock.sendto(packet, (destination, 1))
        send_time = time.time()

        reply = sock.recvfrom(1024)
        recv_time = time.time()

        rtt = round((recv_time - send_time) * 1000, 2)

        icmp_header = reply[0][20:28]
        type, code, checksum, reply_id, reply_seq = struct.unpack('!BBHHH', icmp_header)
        if type == 0: 
            print(f"Resp from {destination}: time={rtt:.2f}ms")
            print(f"type={type}, code={code}, check={checksum}, rep_id={reply_id}, seq={reply_seq}")
            return rtt
        else:
            print(f"Got unexpected response type: type={type}, code={code}")
            return None

    except socket.timeout:
        print(f"Socket timeout for packet {seq_num}")
        return None

def make_echo_req(seq_num: int, id: int):
    header = struct.pack("!BBHHH", 8, 0, 0, id, seq_num)
    check = calc_checksum(header)
    header = struct.pack("!BBHHH", 8, 0, check, id, seq_num)
    return header

def calc_checksum(data: bytes) -> int:
    s = 0
    for i in range(0, len(data), 2):
        w = (data[i] << 8) + (data[i+1] if i+1 < len(data) else 0)
        s += w
        s = (s & 0xffff) + (s >> 16) 
    return ~s & 0xffff


if __name__ == "__main__":
    send_pings(10, "www.vk.ru")