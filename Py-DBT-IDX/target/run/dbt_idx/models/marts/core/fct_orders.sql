
  
    

  create  table "IDX-Stock"."warehouse"."fct_orders__dbt_tmp"
  
  
    as
  
  (
    with orders as (
    select *
    from "IDX-Stock"."warehouse"."stg_eltool__orders"
)
select * from orders
  );
  