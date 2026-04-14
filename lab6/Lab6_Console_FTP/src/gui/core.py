import tkinter as tk
from tkinter import ttk, filedialog, messagebox, simpledialog
from client.login import FTPClient
import os
from dotenv import load_dotenv
from client.entries import parse_ftp_line

class FTPGUI:
    def __init__(self):
        load_dotenv()
        self.root = tk.Tk()
        self.root.title("FTP Client for Lab6")
        self.root.geometry("900x700")
        self.current_path = "/"

        self.client = None

        self._setup_connection_frame()
        self._setup_main_frame()
        self._show_connection_frame()

    def run(self):
        self.root.mainloop()

    def _show_connection_frame(self):
        self.main_frame.pack_forget()
        self.connection_frame.pack(fill=tk.BOTH, expand=True)

    def _show_main_frame(self):
        self.connection_frame.pack_forget()
        self.main_frame.pack(fill=tk.BOTH, expand=True)

    def _setup_connection_frame(self):
        self.connection_frame = ttk.Frame(self.root, padding="20")
        
        ttk.Label(self.connection_frame, text="FTP Connection", font=("Arial", 16, "bold")).grid(row=0, column=0, columnspan=2, pady=10)
        
        ttk.Label(self.connection_frame, text="Server:").grid(row=1, column=0, sticky=tk.W, pady=5)
        self.server_entry = ttk.Entry(self.connection_frame, width=30)
        self.server_entry.grid(row=1, column=1, pady=5)
        
        ttk.Label(self.connection_frame, text="Port:").grid(row=2, column=0, sticky=tk.W, pady=5)
        self.port_entry = ttk.Entry(self.connection_frame, width=30)
        self.port_entry.grid(row=2, column=1, pady=5)
        self.port_entry.insert(0, "21")
        
        ttk.Label(self.connection_frame, text="Username:").grid(row=3, column=0, sticky=tk.W, pady=5)
        self.username_entry = ttk.Entry(self.connection_frame, width=30)
        self.username_entry.grid(row=3, column=1, pady=5)
        
        ttk.Label(self.connection_frame, text="Password:").grid(row=4, column=0, sticky=tk.W, pady=5)
        self.password_entry = ttk.Entry(self.connection_frame, width=30, show="*")
        self.password_entry.grid(row=4, column=1, pady=5)
        
        ttk.Button(self.connection_frame, text="Set default user info", command=self._set_default).grid(row=5, column=0, columnspan=2, pady=5)
        
        ttk.Button(self.connection_frame, text="Connect", command=self._connect).grid(row=6, column=0, columnspan=2, pady=20)
        
        self.connection_status = ttk.Label(self.connection_frame, text="")
        self.connection_status.grid(row=7, column=0, columnspan=2)

    def _setup_main_frame(self):
        self.main_frame = ttk.Frame(self.root)
    
        toolbar = ttk.Frame(self.main_frame)
        toolbar.pack(fill=tk.X, padx=5, pady=5)
        
        ttk.Button(toolbar, text="Refresh", command=self._refresh_list).pack(side=tk.LEFT, padx=2)
        ttk.Button(toolbar, text="Up", command=self._go_up).pack(side=tk.LEFT, padx=2)
        ttk.Button(toolbar, text="New File", command=self._create_new_file).pack(side=tk.LEFT, padx=2)
        ttk.Button(toolbar, text="New Folder", command=self._create_new_folder).pack(side=tk.LEFT, padx=2)
        ttk.Button(toolbar, text="Delete", command=self._delete_item).pack(side=tk.LEFT, padx=2)
        ttk.Button(toolbar, text="Upload", command=self._upload_file).pack(side=tk.LEFT, padx=2)
        ttk.Button(toolbar, text="Download", command=self._download_file).pack(side=tk.LEFT, padx=2)
        ttk.Button(toolbar, text="Disconnect", command=self._disconnect).pack(side=tk.RIGHT, padx=2)
        
        path_frame = ttk.Frame(self.main_frame)
        path_frame.pack(fill=tk.X, padx=5, pady=5)
        
        ttk.Label(path_frame, text="Path:").pack(side=tk.LEFT)
        self.path_var = tk.StringVar(value="/")
        ttk.Entry(path_frame, textvariable=self.path_var, state="readonly").pack(side=tk.LEFT, fill=tk.X, expand=True, padx=5)
        
        list_frame = ttk.Frame(self.main_frame)
        list_frame.pack(fill=tk.BOTH, expand=True, padx=5, pady=5)
        
        columns = ("name", "file_type", "size", "parent")
        self.tree = ttk.Treeview(list_frame, columns=columns, show="tree headings")
        
        self.tree.heading("#0", text="")
        self.tree.heading("name", text="Name")
        self.tree.heading("file_type", text="File type")
        self.tree.heading("size", text="Size")
        self.tree.heading("parent", text="Parent")
        
        self.tree.column("#0", width=0, stretch=False)
        self.tree.column("name", width=400)
        self.tree.column("file_type", width=100)
        self.tree.column("size", width=100)
        self.tree.column("parent", width=150)
        
        scrollbar = ttk.Scrollbar(list_frame, orient=tk.VERTICAL, command=self.tree.yview)
        self.tree.configure(yscrollcommand=scrollbar.set)
        
        self.tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)

        self.tree.bind("<Double-1>", self._on_item_double_click)

        content_frame = ttk.LabelFrame(self.main_frame, text="File Content (Retrieve)", padding="5")
        content_frame.pack(fill=tk.BOTH, expand=True, padx=5, pady=5)
        
        self.content_text = tk.Text(content_frame, height=10, wrap=tk.WORD)
        content_scroll = ttk.Scrollbar(content_frame, orient=tk.VERTICAL, command=self.content_text.yview)
        self.content_text.configure(yscrollcommand=content_scroll.set)
        
        self.content_text.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        content_scroll.pack(side=tk.RIGHT, fill=tk.Y)
        
        retrieve_frame = ttk.Frame(self.main_frame)
        retrieve_frame.pack(fill=tk.X, padx=5, pady=5)
        ttk.Button(retrieve_frame, text="Retrieve Selected File", command=self._retrieve_file).pack()

        self.status_var = tk.StringVar(value="Ready")
        status_bar = ttk.Label(self.main_frame, textvariable=self.status_var, relief=tk.SUNKEN, anchor=tk.W)
        status_bar.pack(side=tk.BOTTOM, fill=tk.X)


    def _set_default(self):
        self.username_entry.delete(0, tk.END)
        self.username_entry.insert(0, os.getenv("FTP_TEST_CLIENT_USERNAME", ""))
        self.password_entry.delete(0, tk.END)
        self.password_entry.insert(0, os.getenv("FTP_TEST_PASSWORD", ""))

    def _connect(self):
        host = self.server_entry.get()
        try:
            port = int(self.port_entry.get())
        except ValueError:
            messagebox.showerror("Error", "Invalid port number")
            return
        username = self.username_entry.get()
        password = self.password_entry.get()

        self.connection_status.config(text="Connecting...")
        try:
            self.client = FTPClient(host, port)
            self.client.authenticate(username, password)
            self.connection_status.config(text="Connected!")
            self._show_main_frame()
            self._refresh_list()
        except Exception as e:
            self.connection_status.config(text=f"Something went wrong: {e}. Try to connect again.")

    def _refresh_list(self):
        if not self.client:
            return
        
        try:
            files = self.client.lst()
            
            self.root.after(0, lambda: self._update_tree(files))
            self.root.after(0, lambda: self.path_var.set(self.current_path))
            self.root.after(0, lambda: self.status_var.set(f"Loaded {len(files)} items"))
        except Exception as e:
            error_msg = str(e)
            self.root.after(0, lambda: self.status_var.set(f"Error: {error_msg}"))

    def _go_up(self):
        try:
            self.client.cdup()
            if self.current_path != "/":
                parts = self.current_path.rstrip('/').split('/')
                self.current_path = '/'.join(parts[:-1]) or "/"
            self._refresh_list()
        except Exception as e:
            error_msg = str(e)
            self.root.after(0, lambda: messagebox.showerror("Error", f"Cannot go up: {error_msg}"))

    def _retrieve_file(self):
            selection = self.tree.selection()
            if not selection:
                messagebox.showwarning("Warning", "Select a file to retrieve")
                return
            
            item = selection[0]
            values = self.tree.item(item, "values")
            if not values:
                return
            
            name = values[0]
            file_type = values[1] 
            
            if file_type == "d":
                messagebox.showwarning("Warning", "Cannot retrieve a directory")
                return
            
            try:
                remote_path = f"{self.current_path}/{name}".replace("//", "/")
                content = self.client.retrieve_file_content(remote_path)
                
                self.root.after(0, lambda: self._display_content(content))
                self.root.after(0, lambda: self.status_var.set(f"Retrieved: {name}"))
            except Exception as e:
                error_msg = str(e)
                self.root.after(0, lambda: messagebox.showerror("Error", error_msg))
            
    def _display_content(self, content):
        self.content_text.delete(1.0, tk.END)
        self.content_text.insert(1.0, content)
        self.content_text.config(state=tk.NORMAL)
        
    def _retrieve_and_edit(self, name):
        remote_path = f"{self.current_path}/{name}".replace("//", "/") 
        try:
            content = self.client.retrieve_file_content(remote_path)
            self.root.after(0, lambda: self._open_editor(remote_path, content))
        except Exception as e:
            self.root.after(0, lambda: messagebox.showerror("Error", str(e)))
    
        
    def _open_editor(self, remote_path, content):
        if str(content).startswith("Warning"):
            return
        def save():
            new_content = text_area.get(1.0, tk.END)
            try:
                print("Trying to save...")
                self.client.update_file_content(remote_path, new_content)
                messagebox.showinfo("Success", "File saved successfully")
                editor.destroy()
                self._refresh_list()
            except Exception as e:
                messagebox.showerror("Error", str(e))

        editor = tk.Toplevel(self.root)
        editor.title(f"Edit: {remote_path}")
        editor.geometry("800x600")
        
        text_area = tk.Text(editor, wrap=tk.WORD)
        text_area.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)
        text_area.insert(1.0, content)

        button_frame = tk.Frame(editor) 
        button_frame.pack(side=tk.BOTTOM, fill=tk.X, padx=10, pady=10)
        
        save_btn = tk.Button(button_frame, text="Save", bg="green", fg="white", 
                            command=save)
        save_btn.pack(side=tk.RIGHT, padx=5)
        
        cancel_btn = tk.Button(button_frame, text="Cancel", bg="gray", fg="white",
                            command=editor.destroy)
        cancel_btn.pack(side=tk.RIGHT, padx=5)         


    def _create_new_file(self):
        name = simpledialog.askstring("New File", "Enter file name (will automatically be txt file):")
        if not name:
            return
        
        remote_path = f"{self.current_path}/{name}.txt".replace("//", "/")
        self._open_editor(remote_path, "") 

    def _create_new_folder(self):
        name = simpledialog.askstring("New Folder", "Enter folder name:")
        if not name:
            return
        
        try:
            self.client.create_directory(name)
            self._refresh_list()
            self.root.after(0, lambda: self.status_var.set(f"Created: {name}"))
        except Exception as e:
            error_msg = str(e)
            self.root.after(0, lambda: messagebox.showerror("Error", error_msg))
    
    def _update_tree(self, files):
        for item in self.tree.get_children():
            self.tree.delete(item)
        
        for file_line in files:
            if not file_line:
                continue
            
            entry = parse_ftp_line(file_line, self.current_path)
            entry_type = "d" if entry.is_directory else "f"
            
            self.tree.insert("", "end", values=(entry.name, entry_type, entry.size, entry.parent), text="")

    def _delete_item(self):
        selection = self.tree.selection()
        if not selection:
            messagebox.showwarning("Warning", "Select an item to delete")
            return
        
        item = selection[0]
        values = self.tree.item(item, "values")
        if not values:
            return
        
        name = values[0]
        file_type = values[1]
        
        if not messagebox.askyesno("Confirm Delete", f"Delete {name}?"):
            return
        
        remote_path = f"{self.current_path}/{name}".replace("//", "/")
        
        try:
            if file_type == "d":
                self.client.delete_directory(name)
            else:
                self.client.delete_file(remote_path)
            self._refresh_list()
            self.root.after(0, lambda: self.status_var.set(f"Deleted: {name}"))
        except Exception as e:
            error_msg = str(e)
            self.root.after(0, lambda: messagebox.showerror("Error", error_msg))

    def _upload_file(self):
        file_path = filedialog.askopenfilename(title="Select file to upload")
        if not file_path:
            return
        
        filename = os.path.basename(file_path)
        remote_path = f"{self.current_path}/{filename}".replace("//", "/")
        
        try:
            self.status_var.set(f"Uploading {filename}...")
            self.client.upload_file(file_path, remote_path)
            self.status_var.set(f"Uploaded {filename}")
            self._refresh_list()
        except Exception as e:
            error_msg = str(e)
            self.root.after(0, lambda: messagebox.showerror("Error", error_msg))

    def _download_file(self):
        selection = self.tree.selection()
        if not selection:
            messagebox.showwarning("Warning", "Select a file to download")
            return
        
        item = selection[0]
        values = self.tree.item(item, "values")
        if not values:
            return
        
        name = values[0]
        file_type = values[1]

        if file_type == "d":
            messagebox.showwarning("Warning", "Cannot download a directory")
            return
        
        save_path = filedialog.asksaveasfilename(title="Save file as", initialfile=name)
        if not save_path:
            return
        
        remote_path = f"{self.current_path}/{name}".replace("//", "/")
        
        try:
            self.status_var.set(f"Downloading {name}...")
            self.client.download_file(remote_path, save_path)
            self.status_var.set(f"Downloaded {name}")
        except Exception as e:
            error_msg = str(e)
            self.root.after(0, lambda: messagebox.showerror("Error", error_msg))


    def _disconnect(self):
        if self.client:
            self.client.disconnect()
        self._show_connection_frame()
        self.status_var.set("Disconnected")
        self.connection_status.config(text="")

    def _on_item_double_click(self, event):
        selection = self.tree.selection()
        if not selection:
            return
        
        item = selection[0]
        values = self.tree.item(item, "values")        
        if not values:
            return
        
        name = values[0]
        file_type = values[1]
        
        if file_type == "d":
            self._cd(name)
        else:
            self._retrieve_and_edit(name)

    def _cd(self, dirname):
        try:
            self.client.cwd(dirname)
            self.current_path = f"{self.current_path}/{dirname}".replace("//", "/")
            self._refresh_list()
        except Exception as e:
            error_msg = str(e)
            self.root.after(0, lambda: messagebox.showerror("Error", f"Cannot enter directory: {error_msg}"))