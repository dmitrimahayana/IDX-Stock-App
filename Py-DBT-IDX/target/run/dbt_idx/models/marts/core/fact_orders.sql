
  
    

  create  table "IDX-Stock"."warehouse"."fact_orders__dbt_tmp"
  
  
    as
  
  (
    with orders as (
    select *
    from "IDX-Stock"."warehouse"."stg_eltool__orders"
)
select * from orders
  );
  