with orders as (
    select *
    from "IDX-Stock"."warehouse"."stg_eltool__orders"
)
select * from orders