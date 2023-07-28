package io.id.stock.analysis;

public class KSQLRunAll {
    public static void main(String[] args) {
        KSQLStockAggregateSink.main(args);
        KSQLCompanyAggregateSink.main(args);
    }
}
