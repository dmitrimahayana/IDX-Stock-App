select
      count(*) as failures,
      count(*) != 0 as should_warn,
      count(*) != 0 as should_error
    from (
      
    
    



select id
from "IDX-Stock"."IDX-Schema"."stg_eltool__company"
where id is null



      
    ) dbt_internal_test