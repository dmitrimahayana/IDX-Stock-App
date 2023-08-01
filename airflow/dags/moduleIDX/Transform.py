import pandas as pd
import numpy as np
from datetime import datetime, timedelta

def transform(stockDf, companyDf):
    print("Start transforming...")
    current_time = datetime.now() + timedelta(hours=7) #server is not using GMT+7
    formatted_time = current_time.strftime('%Y-%m-%d %H:%M:%S')

    stockDf["rank"] = stockDf.groupby("ticker")["date"].rank(ascending=0, method='dense')
    rank1Df = stockDf[stockDf["rank"] == 1]
    rank2Df = stockDf[stockDf["rank"] == 2]
    joinDf1 = pd.merge(rank1Df, rank2Df, left_on='ticker', right_on='ticker', how='inner')
    joinDf2 = pd.merge(joinDf1, companyDf, left_on='ticker', right_on='ticker', how='inner')
    joinDf2["change"] = (joinDf2["close_x"] - joinDf2["close_y"]).round(2)
    joinDf2["changeval"] = np.where(joinDf2["change"] > 0,
                                    "+" + joinDf2["change"].astype(str),
                                    joinDf2["change"].astype(str))
    joinDf2["changepercent"] = np.where(joinDf2["change"] > 0, "+" + ((joinDf2["change"] / joinDf2["close_y"]).round(2)).astype(str) + "%", ((joinDf2["change"] / joinDf2["close_y"]).round(2)).astype(str) + "%")
    joinDf2["postdate"] = formatted_time
    finalDf = joinDf2
    newcoldict = {'id_x': 'id',
                  'date_x': 'date',
                  'open_x': 'open',
                  'high_x': 'high',
                  'low_x': 'low',
                  'close_x': 'close',
                  'volume_x': 'volume',
                  '_id': '_id_z',
                  'id': 'id_z'
                  }

    # call rename () method
    finalDf.rename(columns=newcoldict, inplace=True)
    finalDf = finalDf[["id", "ticker", "date", "open", "high", "low", "close", "volume", "change", "changeval", "changepercent", "name", "logo", "postdate"]]
    print(finalDf.dtypes)
    print("Total Join Table Rows: "+str(finalDf.shape[0]))
    # print(finalDf[["id", "change", "changeval", "changepercent"]].head(5))
    print("End transforming...")
    return finalDf