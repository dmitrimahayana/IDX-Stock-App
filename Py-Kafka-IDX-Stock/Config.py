import os

config = {
    'bootstrap.servers': 'localhost:39092,localhost:39093,localhost:39094'
}
srConfig = {
    'url': 'http://localhost:8282'
}
ksqlConfig = {
    'url': 'http://localhost:9088',
    'mode': 'earliest'  # earliest/latest
}
mongoConfig = {
    'url': 'mongodb://localhost:27017'
}
rootDirectory = os.path.dirname(os.path.abspath(__file__))  # This is Project Root
apiKey = 'pKg7UnAUzqKj8GMKQWu2R83e2N7Jno'
