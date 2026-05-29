import socket
import pickle
import tkinter as tk

class DrawingClient:
    def __init__(self, server_host='127.0.0.1', server_port=12345):
        self.server_host = server_host
        self.server_port = server_port
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.connect((self.server_host, self.server_port))

        self.root = tk.Tk()
        self.root.title("Client - Draw here")
        self.canvas = tk.Canvas(self.root, bg='white', width=800, height=600)
        self.canvas.pack()

        self.last_x, self.last_y = None, None
        self.canvas.bind("<B1-Motion>", self.paint)
        self.canvas.bind("<ButtonRelease-1>", self.reset)

        self.root.protocol("WM_DELETE_WINDOW", self.on_close)

    def paint(self, event):
        x, y = event.x, event.y
        if self.last_x and self.last_y:
            self.canvas.create_line(self.last_x, self.last_y, x, y, width=3, fill='black')

            data = pickle.dumps(('line', self.last_x, self.last_y, x, y))
            self.sock.sendall(len(data).to_bytes(4, 'big') + data)
        self.last_x, self.last_y = x, y

    def reset(self, event):
        self.last_x, self.last_y = None, None

    def on_close(self):
        try:
            data = pickle.dumps(('close',))
            self.sock.sendall(len(data).to_bytes(4, 'big') + data)
        except:
            pass
        self.sock.close()
        self.root.destroy()

    def run(self):
        self.root.mainloop()

if __name__ == "__main__":
    client = DrawingClient()
    client.run()