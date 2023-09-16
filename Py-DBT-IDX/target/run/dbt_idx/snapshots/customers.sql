
      
  
    

  create  table "IDX-Stock"."snapshots"."customers_snapshot"
  
  
    as
  
  (
    

    select *,
        md5(coalesce(cast(customer_id as varchar ), '')
         || '|' || coalesce(cast(datetime_updated as varchar ), '')
        ) as dbt_scd_id,
        datetime_updated as dbt_updated_at,
        datetime_updated as dbt_valid_from,
        nullif(datetime_updated, datetime_updated) as dbt_valid_to
    from (
        



select *
from "IDX-Stock"."IDX-Schema"."customers"

    ) sbq



  );
  
  