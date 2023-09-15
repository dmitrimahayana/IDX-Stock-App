from datetime import timedelta, datetime

from airflow import DAG
from airflow.providers.google.cloud.transfers.postgres_to_gcs import PostgresToGCSOperator
from airflow.providers.google.cloud.transfers.gcs_to_bigquery import GCSToBigQueryOperator
from airflow.providers.google.cloud.operators.bigquery import BigQueryCheckOperator
from airflow.providers.google.cloud.operators.bigquery import BigQueryCreateEmptyTableOperator

## Define the default arguments for the DAG
default_args = {
    'owner': 'Dmitri',
    'start_date': datetime(2023, 9, 12),
    'retries': 1,  # Number of retries if a task fails
    'retry_delay': timedelta(minutes=5),  # Time between retries
}

## Create a DAG instance
dag = DAG(
    'BigQuery_IDX_ETL_V2',
    default_args=default_args,
    description='An Airflow DAG for IDX Stock Postgre to BigQuery',
    schedule_interval=None,  # Set the schedule interval (e.g., None for manual runs)
    catchup=False  # Do not backfill (run past dates) when starting the DAG
)

## BigQuery config variables
BQ_CONN_ID = "google_cloud_default" # Defined in airflow connection
BQ_PROJECT = "ringed-land-398802"
BQ_DATASET = "IDX"
BQ_TABLE1 = "dim_stocks"
BQ_TABLE2 = "dim_companies"
BQ_BUCKET = 'idx-data'

## Postgres config variables
PG_CONN_ID = "postgres_default" # Defined in airflow connection
PG_SCHEMA = "IDX-Schema"
PG_TABLE1 = "ksql-stock-stream"
PG_TABLE2 = "ksql-company-stream"
JSON_FILENAME1 = 'stock_data_' + datetime.now().strftime('%Y-%m-%d') + '.json'
JSON_FILENAME2 = 'company_data_' + datetime.now().strftime('%Y-%m-%d') + '.json'

## Task to transfer data from PostgreSQL to GCS
postgres_stock_to_gcs = PostgresToGCSOperator(
    task_id = 'postgres_stock_to_gcs',
    sql = f'SELECT * FROM "{PG_SCHEMA}"."{PG_TABLE1}";',
    bucket = BQ_BUCKET,
    filename = JSON_FILENAME1,
    export_format = 'CSV',  # You can change the export format as needed
    postgres_conn_id = PG_CONN_ID,  # Set your PostgreSQL connection ID
    field_delimiter=',',  # Optional, specify field delimiter for CSV
    gzip = False,  # Set to True if you want to compress the output file
    task_concurrency = 1,  # Optional, adjust concurrency as needed
    gcp_conn_id = BQ_CONN_ID,  # Set your GCS connection ID
    dag = dag,
)

## Task to transfer data from PostgreSQL to GCS Bucket
postgres_company_to_gcs = PostgresToGCSOperator(
    task_id = 'postgres_company_to_gcs',
    sql = f'SELECT * FROM "{PG_SCHEMA}"."{PG_TABLE2}";',
    bucket = BQ_BUCKET,
    filename = JSON_FILENAME2,
    export_format = 'CSV',  # You can change the export format as needed
    postgres_conn_id = PG_CONN_ID,  # Set your PostgreSQL connection ID
    field_delimiter=',',  # Optional, specify field delimiter for CSV
    gzip = False,  # Set to True if you want to compress the output file
    task_concurrency = 1,  # Optional, adjust concurrency as needed
    gcp_conn_id = BQ_CONN_ID,  # Set your GCS connection ID
    dag = dag,
)

## Task to transfer data from GCS Bucket to BigQuery
bq_stock_load_csv = GCSToBigQueryOperator(
    task_id="bq_stock_load_csv",
    bucket=BQ_BUCKET,
    source_objects=[JSON_FILENAME1],
    destination_project_dataset_table=f"{BQ_PROJECT}.{BQ_DATASET}.{BQ_TABLE1}",
    schema_fields=[
        {"name": "id", "type": "STRING", "mode": "REQUIRED"},
        {"name": "ticker", "type": "STRING", "mode": "NULLABLE"},
        {"name": "date", "type": "STRING", "mode": "NULLABLE"},
        {"name": "open", "type": "FLOAT64", "mode": "NULLABLE"},
        {"name": "high", "type": "FLOAT64", "mode": "NULLABLE"},
        {"name": "low", "type": "FLOAT64", "mode": "NULLABLE"},
        {"name": "close", "type": "FLOAT64", "mode": "NULLABLE"},
        {"name": "volume", "type": "INT64", "mode": "NULLABLE"},
    ],
    create_disposition='CREATE_IF_NEEDED',  # You can change this if needed
    write_disposition="WRITE_TRUNCATE", # You can change this if needed
    gcp_conn_id = BQ_CONN_ID,  # Set your GCS connection ID
    dag = dag,
)

## Task to transfer data from GCS Bucket to BigQuery
bq_company_load_csv = GCSToBigQueryOperator(
    task_id="bq_company_load_csv",
    bucket=BQ_BUCKET,
    source_objects=[JSON_FILENAME2],
    destination_project_dataset_table=f"{BQ_PROJECT}.{BQ_DATASET}.{BQ_TABLE2}",
    schema_fields=[
        {"name": "id", "type": "STRING", "mode": "REQUIRED"},
        {"name": "ticker", "type": "STRING", "mode": "NULLABLE"},
        {"name": "name", "type": "STRING", "mode": "NULLABLE"},
        {"name": "logo", "type": "STRING", "mode": "NULLABLE"},
    ],
    create_disposition='CREATE_IF_NEEDED',  # You can change this if needed
    write_disposition="WRITE_TRUNCATE", # You can change this if needed
    gcp_conn_id = BQ_CONN_ID,  # Set your GCS connection ID
    dag = dag,
)

## Task BigQueryOperator: check that the data table is existed in the dataset
bq_stock_count = BigQueryCheckOperator(
    task_id="bq_stock_count",
    sql=f"SELECT COUNT(*) FROM `{BQ_DATASET}.{BQ_TABLE1}` WHERE `ticker` = 'PGAS' and `date` = '2023-09-14';",
    use_legacy_sql=False,
    gcp_conn_id=BQ_CONN_ID,  # Use your Airflow BigQuery connection ID
    dag=dag
)

## Task BigQueryOperator: check that the data table is existed in the dataset
bq_company_count = BigQueryCheckOperator(
    task_id="bq_company_count",
    sql=f"SELECT COUNT(*) FROM `{BQ_DATASET}.{BQ_TABLE2}`;",
    use_legacy_sql=False,
    gcp_conn_id=BQ_CONN_ID,  # Use your Airflow BigQuery connection ID
    dag=dag
)

## Task BigQueryOperator: check that the data table is existed in the dataset
bq_create_stock_view = BigQueryCreateEmptyTableOperator(
    task_id="bq_create_stock_view",
    dataset_id=BQ_DATASET,
    table_id="stock_view",
    view={
        "query": """
                SELECT 
                *
                FROM (
                    SELECT 
                    *,
                    DENSE_RANK() OVER (PARTITION BY table_row.ticker ORDER BY table_row.rownum desc) AS ranking
                    FROM (
                        SELECT a.id, a.ticker, b.name, a.date, a.open, a.high, a.low, a.close, a.volume, b.logo,
                        ROW_NUMBER() OVER (PARTITION BY a.ticker ORDER BY a.date desc) AS rownum
                        FROM `IDX.dim_stocks` AS a
                        INNER JOIN `IDX.dim_companies` AS b ON a.ticker = b.ticker
                    ) table_row
                ) table_rank
                ORDER BY table_rank.ranking asc, table_rank.ticker asc;
                """,
        "useLegacySql": False,
    },
    gcp_conn_id=BQ_CONN_ID,  # Use your Airflow BigQuery connection ID
    dag=dag
)

## Task BigQueryOperator: check that the data table is existed in the dataset
bq_create_last_stock_view = BigQueryCreateEmptyTableOperator(
    task_id="bq_create_last_stock_view",
    dataset_id=BQ_DATASET,
    table_id="last_stock_view",
    view={
        "query": """
                SELECT
                rank1.id, rank1.ticker, rank1.name, rank1.date, rank1.open, rank1.high, rank1.low, rank1.close,
                rank2.close AS lastclose,
                rank1.close - rank2.close AS changevalue,
                ((rank1.close - rank2.close)/rank2.close) * 100 AS changepercent,
                rank1.volume, rank1.logo
                FROM
                (SELECT * FROM `IDX.stock_view` WHERE ranking = 1) rank1
                INNER JOIN 
                (SELECT * FROM `IDX.stock_view` WHERE ranking = 2) rank2
                ON rank1.ticker = rank2.ticker
                """,
        "useLegacySql": False,
    },
    gcp_conn_id=BQ_CONN_ID,  # Use your Airflow BigQuery connection ID
    dag=dag
)

## Define task
postgres_stock_to_gcs >> bq_stock_load_csv >> bq_stock_count
postgres_company_to_gcs >> bq_company_load_csv >> bq_company_count

bq_stock_count >> bq_create_stock_view
bq_company_count >> bq_create_stock_view

bq_create_stock_view >> bq_create_last_stock_view