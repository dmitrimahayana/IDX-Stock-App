
  create view "IDX-Stock"."warehouse"."stg_eltool__state__dbt_tmp"
    
    
  as (
    with source as (select *
                from "IDX-Stock"."warehouse"."state"),
     renamed as (select state_identifier::INT AS state_id,
                         state_code::VARCHAR(2) AS state_code,
                         st_name::VARCHAR(30) AS state_name
                 from source)
select *
from renamed
  );