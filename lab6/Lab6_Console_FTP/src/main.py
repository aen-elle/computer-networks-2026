from dotenv import load_dotenv
from console.console import run_cli
from gui.core import FTPGUI

TEST_FILE_PATH = "G:\дз\computer-networks-2026\lab6\Lab6_Console_FTP\src\\test_to_send.txt"

def main():
    load_dotenv()
    while True:
        choice = input("Make a choice: run the client with GUI or CLI. 1 - CLI, 2 - GUI.")
        if choice == "1":
            run_cli()
            break
        elif choice == "2":
            FTPGUI().run()
            break
        else:
            print("Unrecognized option; closing the app...")
            break 
            

if __name__ == "__main__":
    main()
