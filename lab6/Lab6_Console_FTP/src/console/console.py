import os
from client.login import FTPClient

def print_help():
    print("\n" + "="*50)
    print("Available commands:")
    print("  ls                  - show all the files and directories")
    print("  upload <local> [remote]  - upload a file. Needs new file name and extension.")
    print("  download <remote> [local] - download a file. Needs new file name and extension.")
    print("  quit                - quit the program.")
    print("  help                - show help.")
    print("="*50 + "\n")

def parse_command(user_input: str):
    parts = user_input.strip().split()
    if not parts:
        return None, []
    return parts[0].lower(), parts[1:]

def run_cli():
    try:
        client = FTPClient()
        username = ""
        password = ""
        
        username_resp = input("Input your username; if left empty, it will be default username specified in .env: ")
        if (len(username_resp) == 0):
            username = os.getenv("FTP_TEST_CLIENT_USERNAME")
        else: 
            username = username_resp

        password_resp = input("Input your password; if left empty, it will be default username specified in .env: ")
        if (len(password_resp) == 0):
            password = os.getenv("FTP_TEST_PASSWORD")
        else: 
            password = password_resp

        try:
            client.authenticate(username, password)
            print(f"Successfully connected as {username}")
        except Exception as e:
            print("Connection failed; restart the app and try again.")
            return
        
        while True:
            try:
                user_input = input("Input a command: ")
                cmd, args = parse_command(user_input)
                
                if not cmd:
                    continue
                
                if cmd == "quit":
                    print("Disconnecting...")
                    break
                
                elif cmd == "help":
                    print_help()
                
                elif cmd == "ls":
                    print("\n" + "="*50)
                    print("Directories and files:")
                    print("="*50)
                    dirs, files = client.list_all_dirs()
                    print("\nDirectories:")
                    print("="*25)
                    for i in dirs:
                        print(i)
                    print("\nFiles:")
                    print("="*25)
                    for i in files:
                        print(i)
                
                elif cmd == "upload":
                    if not args:
                        print("Usage: upload <local_file> [remote_path]")
                    else:
                        try:
                            local_path = args[0]
                            if not os.path.exists(local_path):
                                print(f"Error: Local file '{local_path}' not found!")
                            else:
                                remote_path = args[1] if len(args) > 1 else TEST_FILE_PATH
                                print(f"Uploading {local_path} -> {remote_path}...")
                                client.upload_file(local_path, remote_path)
                                print("Upload finished!")
                        except Exception as e:
                            print(e)
                            continue
                    
                elif cmd == "download":
                    if not args:
                        print("Usage: download <remote_file> [local_path]")
                    else:
                        try: 
                            remote_path = args[0]
                            local_path = args[1] if len(args) > 1 else os.path.basename(remote_path)
                            print(f"Downloading {remote_path} -> {local_path}...")
                            client.download_file(remote_path, local_path)
                            print("Download completed!")
                        except Exception as e:
                            print(e)
                            continue
                    
                else:
                    print(f"Unknown command: '{cmd}'. Type 'help' for available commands.")
                    
            except KeyboardInterrupt:
                print("\n\nInterrupted by user")
                break
    except Exception as e:
        print(f"Error: {e}")
