
  create view "IDX-Stock"."IDX-Schema"."stg_eltool__customers__dbt_tmp"
    
    
  as (
    with source as (select *
                from "IDX-Stock"."IDX-Schema"."customers"),
     renamed as (select
                     customer_id,
                    'This is Dummy' as dummy
                 from source
                 where customer_id = 1234567890)
select *
from renamed
  );