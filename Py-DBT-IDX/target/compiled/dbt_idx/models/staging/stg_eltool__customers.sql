with source as (select *
                from "IDX-Stock"."warehouse"."customers"),
     renamed as (select customer_id,
                        'This is Dummy' as dummy
                 from source)
select *
from renamed