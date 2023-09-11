
  
    

  create  table "IDX-Stock"."IDX-Schema"."fact_orders__dbt_tmp"
  
  
    as
  
  (
    with orders as (
    select *
    from "IDX-Stock"."IDX-Schema"."stg_eltool__orders"
)
select * from orders
  );
  