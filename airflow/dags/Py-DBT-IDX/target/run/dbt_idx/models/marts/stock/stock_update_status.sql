
  create view "IDX-Stock"."IDX-Schema"."stock_update_status__dbt_tmp"
    
    
  as (
    with stock as (select *,
                        rank() over (
                        partition by ticker
                        order by date desc
                        ) as rank
               from "IDX-Stock"."IDX-Schema"."fact_stocks")
select a.id,
       a.ticker,
       a.name,
       a.date,
       a.open,
       a.high,
       a.low,
       a.close,
       a.volume,
       a.rank,
       a.logo
from stock a
order by a.date desc, a.ticker desc
  );