import socket
from .entries import parse_ftp_line, FTPEntry

class FTPClient:
    def __init__(self, host="127.0.0.1", port=21):
        self.host = host
        self.control_port = port
        self.control_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.data_socket = None
        self.data_port = None
        self.connected = False

        self.control_socket.connect((self.host, self.control_port))
        response = self._read_control_data()
        if not self._check_response(response, "220"):
            raise ConnectionError("Connection was not started successfully; please, try again.")
        self._log_response(response)

    def authenticate(self, username: str, password: str):
            self.control_socket.send(f"USER {username}\r\n".encode())
            user_response = self._read_control_data()
            if not self._check_response(user_response, "331"):
                raise ConnectionRefusedError("Unknown user")
            self._log_response(user_response)

            self.control_socket.send(f"PASS {password}\r\n".encode())
            pass_response = self._read_control_data()
            if not self._check_response(pass_response, "230"):
                raise ConnectionRefusedError("Wrong password")
            self._log_response(pass_response)

            self.connected = True

    def list_all_dirs(self):
        found_dirs = []
        found_files = []

        if not self.connected:
            raise ConnectionError("Not connected to the server")
        response = self.lst()
        self._recursive_file_search(response, found_dirs, found_files)
        return found_dirs, found_files
                
    def _recursive_file_search(self, file_list: list[str], found_dirs: list[FTPEntry], found_files: list[FTPEntry], current_dir: str = "root"):
        if file_list[0]:
            for i in file_list:
                entry = parse_ftp_line(i, current_dir)
                if entry is None:
                    raise TypeError("Parsing went wrong")
                if not entry.is_directory:
                    found_files.append(entry)
                else:
                    found_dirs.append(entry)
                    
                    self.cwd(entry.name)
                    files = self.lst()
                    if files[0]:
                        self._recursive_file_search(files, found_dirs, found_files, entry.name)
                    self.cdup()
        

    def upload_file(self, local_path: str, remote_path: str = "/send_text.txt"):
        self._establish_passive_conn()
        self._set_binary_mode()
        self.control_socket.send(f"STOR {remote_path}\r\n".encode())
        response = self._read_control_data()
        print(response)
        if (self._check_response(response, "150")):
            with open(local_path, "rb") as f:
                while True:
                    chunk = f.read(4096)
                    if not chunk:
                        break
                    self.data_socket.send(chunk)
        else: 
            self._close_data_socket()
            raise PermissionError(f"Cannot upload to {remote_path}: {response}")
        self._close_data_socket()

        final_response = self._read_control_data()
        if not final_response.startswith("226"):
            raise ConnectionError(f"Upload transfer failed: {final_response}")

    def download_file(self, remote_path: str, local_path: str):
        self._establish_passive_conn()
        self._set_binary_mode()
        self.control_socket.send(f"RETR {remote_path}\r\n".encode())
    
        retr_response = self._read_control_data()
        if not retr_response.startswith("150"):
            raise ConnectionError(f"RETR failed: {retr_response}")
        
        with open(local_path, 'wb') as f:
            while True:
                chunk = self.data_socket.recv(4096)
                if not chunk:
                    break
                f.write(chunk)

        self._close_data_socket()
        final_response = self._read_control_data()
        if not final_response.startswith("226"):
            raise ConnectionError(f"Download transfer failed: {final_response}")

    def disconnect(self):
        self.control_socket.close()
        if (self.data_socket):
            self.data_socket.close()
        self.connected = False

    def _close_data_socket(self):
        self.data_socket.close()
        self.data_port = None

    def cwd(self, dir_name: str):
        self.control_socket.send(f"CWD {dir_name}\r\n".encode())
        confirmation = self._read_control_data()
        if self._check_response(confirmation, "250"):
            return True
        raise ConnectionError("Something went wrong while performing cwd command.")
    
    def pwd(self):
        self.control_socket.send(b"PWD\r\n")
        confirmation = self._read_control_data()
        print(confirmation)

    def cdup(self):
        self.control_socket.send(b"CDUP\r\n")
        confirmation = self._read_control_data()
        if self._check_response(confirmation, "250"):
            return True
        raise ConnectionError("Something went wrong while performing cdup command.")


    def lst(self):
        self._establish_passive_conn()
        self.control_socket.send(b"LIST\r\n")
        response = self._read_control_data()
        if (self._check_response(response, "150")):
            confirmation = self._read_control_data()
            if (self._check_response(confirmation, "226")):
                files = self._read_data_socket().strip().split("\r\n")
                self._close_data_socket()
                return files
        else:
            raise ConnectionError("Something went wrong while receiving files.")
        
    def retrieve_file_content(self, remote_path: str) -> str:
        self._establish_passive_conn()
        self._set_binary_mode()
        
        self.control_socket.send(f"RETR {remote_path}\r\n".encode())
        retr_response = self._read_control_data()
        
        if retr_response.startswith("550"):
            self._close_data_socket()
            raise FileNotFoundError(f"File not found: {remote_path}")
        
        if not retr_response.startswith("150"):
            self._close_data_socket()
            raise ConnectionError(f"RETR failed: {retr_response}")
        
        content_bytes = b""
        while True:
            chunk = self.data_socket.recv(4096)
            if not chunk:
                break
            content_bytes += chunk
        
        self._close_data_socket()
        final_response = self._read_control_data()
        
        if not final_response.startswith("226"):
            raise ConnectionError(f"Download failed: {final_response}")

        try:
            return content_bytes.decode('utf-8')
        except UnicodeDecodeError:
            return "Warning: this is a binary file that can not be decoded."

    def update_file_content(self, remote_path: str, content: str):
        self._establish_passive_conn()
        self._set_binary_mode()
        
        print("In my client rn...")
        print(remote_path)
        self.control_socket.send(f"STOR {remote_path}\r\n".encode())
        response = self._read_control_data()
        
        if response.startswith("550"):
            self._close_data_socket()
            raise PermissionError(f"Cannot write to {remote_path}: {response}")
        
        if not response.startswith("150"):
            self._close_data_socket()
            raise ConnectionError(f"STOR failed: {response}")
        
        content_bytes = content.encode('utf-8')
        self.data_socket.send(content_bytes)
        
        self._close_data_socket()
        final_response = self._read_control_data()
        
        if not final_response.startswith("226"):
            raise ConnectionError(f"Upload failed: {final_response}")

    def delete_file(self, remote_path: str):
        self.control_socket.send(f"DELE {remote_path}\r\n".encode())
        response = self._read_control_data()
        
        if self._check_response(response, "550"):
            raise FileNotFoundError(f"File not found or cannot delete: {remote_path}")
        if not self._check_response(response, "250"):
            raise ConnectionError(f"DELE failed: {response}")

    def rename_file(self, old_path: str, new_path: str):
        self.control_socket.send(f"RNFR {old_path}\r\n".encode())
        rnfr_response = self._read_control_data()
        
        if not self._check_response(rnfr_response, "350"):
            raise FileNotFoundError(f"File not found: {old_path}")
        
        self.control_socket.send(f"RNTO {new_path}\r\n".encode())
        rnto_response = self._read_control_data()
        
        if not self._check_response(rnto_response, "250"):
            raise ConnectionError(f"Rename failed: {rnto_response}")

    def create_directory(self, dir_name: str):
        self.control_socket.send(f"MKD {dir_name}\r\n".encode())
        response = self._read_control_data()
        
        if not self._check_response(response, "257"):
            raise ConnectionError(f"MKD failed: {response}")

    def delete_directory(self, dir_name: str):
        self.control_socket.send(f"RMD {dir_name}\r\n".encode())
        response = self._read_control_data()
        
        if not self._check_response(response, "250"):
            raise ConnectionError(f"RMD failed: {response}")
            
    
    def _set_binary_mode(self):
        self.control_socket.send(b"TYPE I\r\n")
        response = self._read_control_data()
        if not response.startswith("200"):
            raise ConnectionError("Failed to set binary mode.")


    def _check_response(self, response: str, code: str):
        return response.startswith(code)
    
    def _log_response(self, response: str):
        print(f"Server: {response} \n")

    def _establish_passive_conn(self):
        if not self.connected:
            raise ConnectionError("Not connected to the server")
        
        self.control_socket.send(b"PASV\r\n")
        pasv_response = self._read_control_data()
        if not self._check_response(pasv_response, "227"):
                raise ConnectionRefusedError("Establishing passive connection went wrong")
        self._log_response(pasv_response)
        self.data_port = self._parse_pasv_response(pasv_response)

        self.data_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.data_socket.connect((self.host, self.data_port))


    def _parse_pasv_response(self, pasv_repsonse: str):
         index = pasv_repsonse.find("(")
         tup = pasv_repsonse[index:].strip().replace("(", "").replace(")", "").split(",")
         return (int(tup[-2]) * 256) + int(tup[-1])

   
    def _read_control_data(self) -> str:
        response = ""
        while True:
            chunk = self.control_socket.recv(4096).decode()
            response += chunk
            
            if len(response) >= 4:
                lines = response.splitlines()
                if lines:
                    last_line = lines[-1]
                    if len(last_line) >= 4 and last_line[3] == ' ':
                        break
            if not chunk:
                break
        return response

    def _read_data_socket(self) -> str:
        data = b""
        while True:
            chunk = self.data_socket.recv(4096)
            if not chunk:  
                break
            data += chunk
        return data.decode()
    

