with source as (select *
                from {{ source('IDX-Schema', 'ksql-company-stream') }}),
     renamed as (select id,
                        ticker,
                        name,
                        logo
                 from source)
select *
from renamed