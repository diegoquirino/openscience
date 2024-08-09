from datetime import datetime
import logging


logging.basicConfig(filename='error_log.txt',
                    level=logging.ERROR,
                    format='%(asctime)s %(levelname)s: %(message)s',
                    datefmt='%Y-%m-%d %H:%M:%S')


def get_formatted_current_datetime():
    now = datetime.now()
    now_formatted_datetime = now.strftime("%Y%m%d%H%M")
    return now_formatted_datetime


def get_software_data(filepath):
    config = {}
    with open(filepath, 'r') as f:
        for line in f:
            key, value = line.split('=')
            try:
                config[key] = eval(value)
            except (NameError, SyntaxError):
                config[key] = value.strip("'")
    return config['PREFIX'], config['SOFTWARE'], config['VERSIONS'], config['ROUNDS']


def log_error(message=""):
    logging.error(message)
    print(message)
