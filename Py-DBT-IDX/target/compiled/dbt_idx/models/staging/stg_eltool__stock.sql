with source as (select DISTINCT *
                from "IDX-Stock"."IDX-Schema"."ksql-stock-stream"),
     renamed as (select *
                 from source)
select *
from renamed