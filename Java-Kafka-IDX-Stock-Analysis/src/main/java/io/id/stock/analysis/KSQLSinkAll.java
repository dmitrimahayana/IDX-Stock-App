package io.id.stock.analysis;

public class KSQLSinkAll {
    public static void main(String[] args) {
        KSQLAggregateSinkCompany ksqlAggregateSinkCompany = new KSQLAggregateSinkCompany();
        KSQLAggregateSinkStock ksqlAggregateSinkStock = new KSQLAggregateSinkStock();
        KSQLAggregateSinkJoinStockCompany ksqlAggregateSinkJoinStockCompany = new KSQLAggregateSinkJoinStockCompany();

        ksqlAggregateSinkCompany.start();
        ksqlAggregateSinkStock.start();
        ksqlAggregateSinkJoinStockCompany.start();
    }
}
