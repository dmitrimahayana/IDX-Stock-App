{% snapshot customers_snapshot %}

{{
    config(
      target_database='IDX-Stock',
      target_schema='snapshots',
      unique_key='customer_id',

      strategy='timestamp',
      updated_at='datetime_updated',
    )
}}

select *
from {{ source('IDX-Schema', 'customers') }}

{% endsnapshot %}