
    
    

with all_values as (

    select
        order_status as value_field,
        count(*) as n_records

    from "IDX-Stock"."IDX-Schema"."customer_orders_status"
    group by order_status

)

select *
from all_values
where value_field not in (
    'delivered','invoiced','shipped','processing','canceled','unavailable'
)


