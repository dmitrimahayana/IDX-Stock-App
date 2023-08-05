package io.id.stock.analysis;

public class KSQLSinkAll {
    public static void main(String[] args) {
        KSQLCompanyAggregateSink.main(args);
        KSQLStockAggregateSink.main(args);
    }
}
