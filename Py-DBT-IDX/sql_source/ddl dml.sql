-- SCHEMA: IDX-Schema

-- DROP SCHEMA IF EXISTS "IDX-Schema" ;
SELECT * FROM "IDX-Schema".account
WHERE created_on > '2023-08-28';

CREATE SCHEMA IF NOT EXISTS "IDX-Schema"
    AUTHORIZATION postgres;
	
CREATE TABLE IF NOT EXISTS "IDX-Schema".account
(
    id integer NOT NULL GENERATED ALWAYS AS IDENTITY ( INCREMENT 1 START 1 MINVALUE 1 MAXVALUE 2147483647 CACHE 1 ),
    username character varying(50) COLLATE pg_catalog."default",
    password character varying(50) COLLATE pg_catalog."default",
    email character varying(255) COLLATE pg_catalog."default",
    created_on timestamp without time zone,
    last_login timestamp without time zone,
    CONSTRAINT account_pkey PRIMARY KEY (id)
)