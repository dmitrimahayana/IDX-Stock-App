import os

config = {
    'bootstrap.servers': 'localhost:39092,localhost:39093,localhost:39094'
}
srConfig = {
    'url': 'http://localhost:8282'
}

rootDirectory = os.path.dirname(os.path.abspath(__file__)) # This is Project Root