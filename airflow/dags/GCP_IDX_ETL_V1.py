from datetime import timedelta, datetime

from airflow import DAG
from airflow.providers.google.cloud.operators.bigquery import BigQueryInsertJobOperator
from airflow.providers.google.cloud.operators.bigquery import BigQueryCreateEmptyTableOperator
from airflow.providers.google.cloud.operators.bigquery import BigQueryCheckOperator

## Define the default arguments for the DAG
default_args = {
    'owner': 'Dmitri',
    'start_date': datetime(2023, 9, 12),
    'retries': 1,  # Number of retries if a task fails
    'retry_delay': timedelta(minutes=5),  # Time between retries
}

## Create a DAG instance
dag = DAG(
    'BigQuery_IDX_ETL_V1',
    default_args=default_args,
    description='An Airflow DAG for BigQuery IDX',
    schedule_interval=None,  # Set the schedule interval (e.g., None for manual runs)
    catchup=False  # Do not backfill (run past dates) when starting the DAG
)

## Config variables
BQ_CONN_ID = "google_cloud_default"
BQ_PROJECT = "ringed-land-398802"
BQ_DATASET = "IDX"
BQ_TABLE = "Stocks"

## Task BigQueryOperator: Create Table
bq_create_table_stock = BigQueryCreateEmptyTableOperator(
    task_id="bq_create_table_stock",
    dataset_id=BQ_DATASET,
    table_id=BQ_TABLE,
    schema_fields=[
        {"name": "id", "type": "STRING", "mode": "REQUIRED"},
        {"name": "ticker", "type": "STRING", "mode": "REQUIRED"},
        {"name": "name", "type": "STRING", "mode": "NULLABLE"},
        {"name": "date", "type": "STRING", "mode": "REQUIRED"},
        {"name": "open", "type": "FLOAT64", "mode": "NULLABLE"},
        {"name": "high", "type": "FLOAT64", "mode": "NULLABLE"},
        {"name": "low", "type": "FLOAT64", "mode": "NULLABLE"},
        {"name": "close", "type": "FLOAT64", "mode": "NULLABLE"},
        {"name": "volume", "type": "INT64", "mode": "NULLABLE"},
        {"name": "rank", "type": "INTEGER", "mode": "NULLABLE"},
        {"name": "logo", "type": "STRING", "mode": "NULLABLE"},
    ],
    gcp_conn_id=BQ_CONN_ID,  # Use your Airflow BigQuery connection ID
    dag=dag
)

INSERT_ROWS_QUERY = (
    f"INSERT {BQ_DATASET}.{BQ_TABLE} VALUES "
    "('PGAS_2023-09-14', 'PGAS', 'PT Perusahaan Gas Negara Tbk.', '2023-09-14', 1445, 1480, 1445, 1460, 56996900, 1, 'https://s3.goapi.id/logo/PGAS.jpg'), "
    "('BBCA_2023-09-14', 'BBCA', 'PT Bank Central Asia Tbk.', '2023-09-14', 9000, 9025, 9000, 9000, 8994400, 1, 'https://s3.goapi.id/logo/BBCA.jpg'); "
)

## Task BigQueryOperator: insert data into table
bq_insert_query_job = BigQueryInsertJobOperator(
    task_id="bq_insert_query_job",
    configuration={
        "query": {
            "query": INSERT_ROWS_QUERY,
            "useLegacySql": False,
        },
        "writeDisposition": 'WRITE_EMPTY',
    },
    gcp_conn_id=BQ_CONN_ID,  # Use your Airflow BigQuery connection ID
    dag=dag
)

## Task BigQueryOperator: check that the data table is existed in the dataset
bq_check_count = BigQueryCheckOperator(
    task_id="bq_check_count",
    sql=f"SELECT COUNT(*) FROM {BQ_DATASET}.{BQ_TABLE}",
    use_legacy_sql=False,
    gcp_conn_id=BQ_CONN_ID,  # Use your Airflow BigQuery connection ID
    dag=dag
)

## Task BigQueryOperator: check that the data table is existed in the dataset
bq_create_materialized_view = BigQueryCreateEmptyTableOperator(
    task_id="bq_create_materialized_view",
    dataset_id=BQ_DATASET,
    table_id="stock_materialized_view",
    materialized_view={
        "query": f"SELECT * FROM `{BQ_PROJECT}.{BQ_DATASET}.{BQ_TABLE}` where rank = 1 or rank = 2",
        "enableRefresh": True,
        "refreshIntervalMs": 2000000,
    },
    gcp_conn_id=BQ_CONN_ID,  # Use your Airflow BigQuery connection ID
    dag=dag
)

## Define task
bq_create_table_stock >> bq_insert_query_job >> bq_check_count >> bq_create_materialized_view