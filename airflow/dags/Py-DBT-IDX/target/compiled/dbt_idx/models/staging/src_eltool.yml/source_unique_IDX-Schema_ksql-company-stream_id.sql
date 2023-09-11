
    
    

select
    id as unique_field,
    count(*) as n_records

from "IDX-Stock"."IDX-Schema"."ksql-company-stream"
where id is not null
group by id
having count(*) > 1


