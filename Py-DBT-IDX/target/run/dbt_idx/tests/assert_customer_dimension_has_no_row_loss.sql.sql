select
      count(*) as failures,
      count(*) != 0 as should_warn,
      count(*) != 0 as should_error
    from (
      -- dim_customers must have the same number of rows as its staging counterpart
-- Therefore return records where this isn't true to make the test fail
select *
from (
    select dim_cust.customer_id
    from "IDX-Stock"."warehouse"."dim_customers" dim_cust
        left join "IDX-Stock"."warehouse"."stg_eltool__customers_snapshots" stg_cust
    on dim_cust.customer_id = stg_cust.customer_id
    where stg_cust.customer_id is null
    UNION ALL
    select stg_cust.customer_id
    from "IDX-Stock"."warehouse"."stg_eltool__customers_snapshots" stg_cust
      left join "IDX-Stock"."warehouse"."dim_customers" dim_cust
    on stg_cust.customer_id = dim_cust.customer_id
    where dim_cust.customer_id is null
) tmp
      
    ) dbt_internal_test