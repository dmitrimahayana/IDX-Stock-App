package io.id.stock.analysis;

public class KSQLSinkAll {
    public static void main(String[] args) {
//        KSQLAggregateSinkCompany.main(args);
//        KSQLAggregateSinkStock.main(args);
        KSQLAggregateSinkCompany ksqlAggregateSinkCompany = new KSQLAggregateSinkCompany();
        KSQLAggregateSinkStock ksqlAggregateSinkStock = new KSQLAggregateSinkStock();

        ksqlAggregateSinkCompany.start();
        ksqlAggregateSinkStock.start();
    }
}
