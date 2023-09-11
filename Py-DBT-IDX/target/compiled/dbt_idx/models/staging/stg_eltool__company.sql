with source as (select *
                from "IDX-Stock"."IDX-Schema"."ksql-company-stream"),
     renamed as (select id,
                        ticker,
                        name,
                        logo
                 from source)
select *
from renamed