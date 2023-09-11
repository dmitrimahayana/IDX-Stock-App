select
      count(*) as failures,
      count(*) != 0 as should_warn,
      count(*) != 0 as should_error
    from (
      -- fact_stocks must have the same number of rows as its staging counterpart
-- Therefore return records where this isn't true to make the test fail
select *
from (
    select stock.id
    from "IDX-Stock"."IDX-Schema"."fact_stocks" stock
    where stock.id is null
) tmp
      
    ) dbt_internal_test