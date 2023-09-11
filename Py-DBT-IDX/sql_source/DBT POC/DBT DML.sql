/*
COPY warehouse.customers(customer_id, zipcode, city, state_code, datetime_created, datetime_updated)
FROM '/DBT_POC/customer_new.csv' DELIMITER ',' CSV HEADER;
*/

--select * from warehouse.customers where customer_id=82;
select * from snapshots.customers_snapshot where customer_id=82;