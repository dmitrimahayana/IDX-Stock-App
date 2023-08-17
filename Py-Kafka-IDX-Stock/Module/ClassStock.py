class Stock(object):
    def __init__(self, id, ticker, date, open, high, low, close, volume):
        self.id = id
        self.ticker = ticker
        self.date = date
        self.open = open
        self.high = high
        self.low = low
        self.close = close
        self.volume = volume


def stockToDict(Stock, ctx):
    return {"id": Stock.id,
            "ticker": Stock.ticker,
            "date": Stock.date,
            "open": Stock.open,
            "high": Stock.high,
            "low": Stock.low,
            "close": Stock.close,
            "volume": Stock.volume}


def dictToStock(dict, ctx):
    return dict
    # return Stock(dict["id"],
    #              dict["ticker"],
    #              dict["date"],
    #              dict["open"],
    #              dict["high"],
    #              dict["low"],
    #              dict["close"],
    #              dict["volume"])
