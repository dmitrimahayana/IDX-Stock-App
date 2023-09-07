with orders as (
    select *
    from "IDX-Stock"."IDX-Schema"."stg_eltool__orders"
)
select * from orders