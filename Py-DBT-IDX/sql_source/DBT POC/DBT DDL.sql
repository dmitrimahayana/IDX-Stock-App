CREATE SCHEMA IF NOT EXISTS snapshots;
DROP TABLE IF EXISTS "IDX-Schema".customers;
CREATE TABLE "IDX-Schema".customers (
    customer_id INT,
    zipcode VARCHAR(5),
    city VARCHAR(100),
    state_code VARCHAR(2),
    datetime_created VARCHAR(100),
    datetime_updated VARCHAR(100)
);
DROP TABLE IF EXISTS "IDX-Schema".orders;
CREATE TABLE "IDX-Schema".orders (
    order_id VARCHAR(32),
    cust_id INT,
    order_status VARCHAR(15),
    order_purchase_timestamp VARCHAR(100),
    order_approved_at VARCHAR(100),
    order_delivered_carrier_date VARCHAR(100),
    order_delivered_customer_date VARCHAR(100),
    order_estimated_delivery_date VARCHAR(100)
);
DROP TABLE IF EXISTS "IDX-Schema".state;
CREATE TABLE "IDX-Schema".state (
    state_identifier VARCHAR(10),
    state_code VARCHAR(5000),
    st_name VARCHAR(5000)
);
/* Please make sure to copy customer, state, orders csv to pg database folder /DBT_POC/ */
COPY "IDX-Schema".customers(customer_id, zipcode, city, state_code, datetime_created, datetime_updated)
FROM '/DBT_POC/customer.csv' DELIMITER ',' CSV HEADER;
COPY "IDX-Schema".state(state_identifier, state_code, st_name)
FROM '/DBT_POC/state.csv' DELIMITER ',' CSV HEADER;
COPY "IDX-Schema".orders(
    order_id,
    cust_id,
    order_status,
    order_purchase_timestamp,
    order_approved_at,
    order_delivered_carrier_date,
    order_delivered_customer_date,
    order_estimated_delivery_date
)
FROM '/DBT_POC/orders.csv' DELIMITER ',' CSV HEADER;