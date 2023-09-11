with source as (select *
                from "IDX-Stock"."IDX-Schema"."ksql-stock-stream"),
     renamed as (select *
                 from source)
select *
from renamed