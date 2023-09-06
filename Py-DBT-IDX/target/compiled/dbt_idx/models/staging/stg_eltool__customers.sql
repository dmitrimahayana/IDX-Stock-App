with source as (select *
                from "IDX-Stock"."warehouse"."customers"),
     renamed as (select
                     customer_id,
                    'This is Dummy' as dummy
                 from source
                 where customer_id = 1234567890)
select *
from renamed