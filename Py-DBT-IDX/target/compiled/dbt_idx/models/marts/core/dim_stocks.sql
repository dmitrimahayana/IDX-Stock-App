-- noinspection SqlDialectInspectionForFile

with stock as (
    select *
    from "IDX-Stock"."IDX-Schema"."stg_eltool__stock"
    ),
     company as (
         select *
         from "IDX-Stock"."IDX-Schema"."stg_eltool__company"
     )
select a.id,
       a.ticker,
       b.name,
       a.date,
       a.open,
       a.high,
       a.low,
       a.close,
       a.volume,
       b.logo
from stock a
    join company b on a.ticker = b.ticker