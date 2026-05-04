import socket
import netifaces

def get_ip_and_mask():
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        sock.connect(("8.8.8.8", 80))
        ip = sock.getsockname()[0]
    finally:
        sock.close()

    default = netifaces.gateways()["default"][netifaces.AF_INET][1]

    addresses = netifaces.ifaddresses(default)
    mask = addresses[netifaces.AF_INET][0]["netmask"]

    return ip, mask

def main():
    ip, mask = get_ip_and_mask()

    print(f"Your IP is {ip}\n")
    print(f"Your netmask is {mask}")


if __name__ == "__main__":
    main()