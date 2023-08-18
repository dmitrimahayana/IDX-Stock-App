import requests
from Config import apiKey


def getAPIResults(apiURL):
    headers = {
        'Accept': '*/*',
        'X-API-KEY': apiKey,
        'User-Agent': 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11',
    }
    response = requests.get(apiURL, headers=headers)

    if response.status_code != 200:
        print('ERROR with Response Code: ' + str(response.status_code) + ' URL: ' + apiURL)
    else:
        return response.json()['data']['results']


if __name__ == '__main__':
    print("THIS IS MAIN")