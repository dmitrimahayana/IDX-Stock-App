with source as (select DISTINCT *
                from {{ source('IDX-Schema', 'ksql-stock-stream') }}),
     renamed as (select *
                 from source)
select *
from renamed