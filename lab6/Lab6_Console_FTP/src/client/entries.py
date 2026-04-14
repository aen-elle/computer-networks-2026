class FTPEntry:
    def __init__(self, is_directory: bool, name: str, size: int, parent: str):
        self.is_directory = is_directory
        self.name = name
        self.size = size
        self.parent = parent

    def __str__(self):
        return f"""(
        name: {self.name}
        is_directory: {self.is_directory}
        size: {self.size}
        parent: {self.parent}\n)"""

def parse_ftp_line(line, current_dir: str = "root") -> FTPEntry|None:
    parts = line.split()
    if len(parts) < 8:
        return None
    
    is_directory = line.startswith('d')
    size = int(parts[4]) if parts[4].isdigit() else 0
    filename = parts[-1]
    
    return FTPEntry(
        is_directory,
        filename,
        int(size),
        current_dir
    )


