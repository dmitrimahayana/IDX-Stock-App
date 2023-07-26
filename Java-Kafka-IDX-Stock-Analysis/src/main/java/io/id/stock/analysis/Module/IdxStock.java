package io.id.stock.analysis.Module;

import java.math.BigInteger;

public class IdxStock {

    public String id;
    public String ticker;
    public String date;
    public Double open;
    public Double high;
    public Double low;
    public Double close;
    public BigInteger volume;

    public IdxStock(String id, String ticker, String date, Double open, Double high, Double low, Double close, BigInteger volume){
        this.id = id;
        this.ticker = ticker;
        this.date = date;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

}
