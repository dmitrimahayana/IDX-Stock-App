
      update "IDX-Stock"."snapshots"."customers_snapshot"
    set dbt_valid_to = DBT_INTERNAL_SOURCE.dbt_valid_to
    from "customers_snapshot__dbt_tmp162339268558" as DBT_INTERNAL_SOURCE
    where DBT_INTERNAL_SOURCE.dbt_scd_id::text = "IDX-Stock"."snapshots"."customers_snapshot".dbt_scd_id::text
      and DBT_INTERNAL_SOURCE.dbt_change_type::text in ('update'::text, 'delete'::text)
      and "IDX-Stock"."snapshots"."customers_snapshot".dbt_valid_to is null;

    insert into "IDX-Stock"."snapshots"."customers_snapshot" ("customer_id", "zipcode", "city", "state_code", "datetime_created", "datetime_updated", "dbt_updated_at", "dbt_valid_from", "dbt_valid_to", "dbt_scd_id")
    select DBT_INTERNAL_SOURCE."customer_id",DBT_INTERNAL_SOURCE."zipcode",DBT_INTERNAL_SOURCE."city",DBT_INTERNAL_SOURCE."state_code",DBT_INTERNAL_SOURCE."datetime_created",DBT_INTERNAL_SOURCE."datetime_updated",DBT_INTERNAL_SOURCE."dbt_updated_at",DBT_INTERNAL_SOURCE."dbt_valid_from",DBT_INTERNAL_SOURCE."dbt_valid_to",DBT_INTERNAL_SOURCE."dbt_scd_id"
    from "customers_snapshot__dbt_tmp162339268558" as DBT_INTERNAL_SOURCE
    where DBT_INTERNAL_SOURCE.dbt_change_type::text = 'insert'::text;

  