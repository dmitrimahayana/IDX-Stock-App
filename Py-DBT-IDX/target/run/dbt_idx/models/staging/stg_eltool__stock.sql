
  create view "IDX-Stock"."IDX-Schema"."stg_eltool__stock__dbt_tmp"
    
    
  as (
    with source as (select *
                from "IDX-Stock"."IDX-Schema"."ksql-stock-stream"),
     renamed as (select *
                 from source)
select *
from renamed
  );