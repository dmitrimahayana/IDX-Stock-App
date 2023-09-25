import requests
from Config import apiKey


def getAPIResults(apiURL):
    headers = {
        'Accept': '*/*',
        'X-API-KEY': apiKey,
        # 'User-Agent': 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11',
    }
    response = requests.get(apiURL, headers=headers)

    if response.status_code != 200:
        print('ERROR with Response Code: ' + str(response.status_code) + ' URL: ' + apiURL)
        return response.status_code, ""
    else:
        return response.status_code, response.json()['data']['results']


if __name__ == '__main__':
    print("THIS IS MAIN")

    # baseUrl = 'https://api.goapi.id/v1/stock/idx/'
    # apiUrl = baseUrl + 'trending'
    # listTrending = getAPIResults(apiUrl)
    # print(listTrending)
    #
    # apiUrl2 = baseUrl + 'companies'
    # listCompany = getAPIResults(apiUrl2)
    # print(listCompany)

    # historicalURL = "https://api.goapi.id/v1/stock/idx/UNTR/historical?from=2023-08-01&to=2023-09-25"
    # listHist = getAPIResults(historicalURL)
    # print(listHist)