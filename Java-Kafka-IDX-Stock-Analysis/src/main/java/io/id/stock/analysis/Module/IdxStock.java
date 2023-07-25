package io.id.stock.analysis.Module;

import java.math.BigInteger;

public class IdxStock {

    private String id;
    private String ticker;
    private String date;
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private BigInteger volume;

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
