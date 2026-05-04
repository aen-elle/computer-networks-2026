import socket
import sys 
from concurrent.futures import ThreadPoolExecutor

def validate_args(args: list[str]):
    if len(args) >= 3:
        start_port = int(args[1])
        end_port = int(args[2])
        if start_port > 0 and end_port <= 65535 and start_port < end_port:
            return True
    raise ValueError("Malformed inputs, try again.")

def check_port(ip: str, port: int, timeout=2.5):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.settimeout(timeout)

    res = sock.connect_ex((ip, port))
    sock.close()
    return port if res == 0 else None


def get_open_ports(ip: str, start_port: int, end_port: int):
    open_ports = []
    with ThreadPoolExecutor(max_workers=200) as ex:
        tasks = [ex.submit(check_port, ip, port) for port in range(start_port, end_port + 1)]
        for task in tasks:
            res = task.result()
            if res:
                open_ports.append(res)
    return sorted(open_ports)

def main():
    args = sys.argv[1:]
    try: 
        validate_args(args)
    except ValueError as e:
        print(e)
        sys.exit()

    ip = args[0]
    start_port = int(args[1])
    end_port = int(args[2])
    print(f"Starting scan from {start_port} to {end_port}...")
    res = get_open_ports(ip, start_port, end_port)
    print(res)
    
if __name__ == "__main__":
    main()