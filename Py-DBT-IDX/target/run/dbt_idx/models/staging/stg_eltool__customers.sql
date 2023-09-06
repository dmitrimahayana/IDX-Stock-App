
  create view "IDX-Stock"."warehouse"."stg_eltool__customers__dbt_tmp"
    
    
  as (
    with source as (select *
                from "IDX-Stock"."warehouse"."customers"),
     renamed as (select customer_id,
                        'This is Dummy' as dummy
                 from source)
select *
from renamed
  );