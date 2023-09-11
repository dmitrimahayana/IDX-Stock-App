
  create view "IDX-Stock"."IDX-Schema"."stg_eltool__company__dbt_tmp"
    
    
  as (
    with source as (select *
                from "IDX-Stock"."IDX-Schema"."ksql-company-stream"),
     renamed as (select id,
                        ticker,
                        name,
                        logo
                 from source)
select *
from renamed
  );