import socket
import pickle
import threading
import tkinter as tk

class DrawingServer:
    def __init__(self, host='0.0.0.0', port=12345):
        self.host = host
        self.port = port
        self.root = tk.Tk()
        self.root.title("Server - Remote Drawing")
        self.canvas = tk.Canvas(self.root, bg='white', width=800, height=600)
        self.canvas.pack()

        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.bind((self.host, self.port))
        self.sock.listen(5)
        print(f"Server listening on {self.host}:{self.port}")

        self.client_sock = None
        self.running = True

        self.accept_thread = threading.Thread(target=self.accept_client, daemon=True)
        self.accept_thread.start()

        self.root.protocol("WM_DELETE_WINDOW", self.on_close)

    def accept_client(self):
        while self.running:
            try:
                client, addr = self.sock.accept()
                print(f"Client connected from {addr}")
                self.client_sock = client
                self.receive_thread = threading.Thread(target=self.receive_data, daemon=True)
                self.receive_thread.start()
                break
            except:
                break

    def receive_data(self):
        while self.running and self.client_sock:
            try:
                raw_len = self.client_sock.recv(4)
                if not raw_len:
                    break
                msg_len = int.from_bytes(raw_len, 'big')
                data = b''
                while len(data) < msg_len:
                    chunk = self.client_sock.recv(msg_len - len(data))
                    if not chunk:
                        break
                    data += chunk
                if len(data) == msg_len:
                    obj = pickle.loads(data)
                    if obj[0] == 'line':
                        _, x1, y1, x2, y2 = obj
                        self.canvas.create_line(x1, y1, x2, y2, width=3, fill='black')
                    elif obj[0] == 'close':
                        break
            except:
                break
        print("Client disconnected")

    def on_close(self):
        self.running = False
        if self.client_sock:
            self.client_sock.close()
        self.sock.close()
        self.root.destroy()

    def run(self):
        self.root.mainloop()

if __name__ == "__main__":
    server = DrawingServer()
    server.run()