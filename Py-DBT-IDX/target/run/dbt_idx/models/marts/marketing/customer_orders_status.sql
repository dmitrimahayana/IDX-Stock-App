
  create view "IDX-Stock"."warehouse"."customer_orders_status__dbt_tmp"
    
    
  as (
    with orders as (select *
                from "IDX-Stock"."warehouse"."fact_orders"),
     customers as (select *
                   from "IDX-Stock"."warehouse"."dim_customers")
select o.order_id,
       o.customer_id,
       o.order_status,
       o.order_purchase_timestamp,
       o.order_approved_at,
       o.order_delivered_carrier_date,
       o.order_delivered_customer_date,
       o.order_estimated_delivery_date,
       c.zipcode    as customer_zipcode,
       c.city       as customer_city,
       c.state_code as customer_state_code,
       c.state_name as customer_state_name
from orders o
    join customers c on o.customer_id = c.customer_id
    and o.order_purchase_timestamp >= c.valid_from
    and o.order_purchase_timestamp <= c.valid_to
  );