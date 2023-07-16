BASE_FILE = "rgp_diarias_diffs.CSV"


def convert_file_utf8():
    # Read the file using the original encoding
    with open(BASE_FILE, 'r', encoding='latin-1') as file:
        file_content = file.read()
    # Write the file back with UTF-8 encoding
    with open(BASE_FILE, 'w', encoding='utf-8', errors='ignore') as file:
        file.write(file_content)


if __name__ == '__main__':
    convert_file_utf8()
