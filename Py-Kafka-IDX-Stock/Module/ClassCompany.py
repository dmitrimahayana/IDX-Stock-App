class Company(object):
    def __init__(self, id, ticker, name, logo, createdtime):
        self.id = id
        self.ticker = ticker
        self.name = name
        self.logo = logo
        self.createdtime = createdtime


def companyToDict(Company, ctx):
    return {"id": Company.id,
            "ticker": Company.ticker,
            "name": Company.name,
            "logo": Company.logo,
            "createdtime": Company.createdtime}


def dictToCompany(dict, ctx):
    return dict
    # return Company(dict["id"],
    #                dict["ticker"],
    #                dict["name"],
    #                dict["logo"])
