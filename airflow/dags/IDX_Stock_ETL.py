from airflow import DAG
from airflow.decorators import task
from airflow.utils.dates import days_ago
import moduleIDX.Extract as extract
import moduleIDX.Transform as transform
import moduleIDX.Load as load
from datetime import timedelta

default_args = {
    'owner': 'Dmitri',
    'start_date': days_ago(1),
}

@task()
def mongodb_extractStock():
    stockDf = extract.extractKSQLStock({})
    return stockDf

@task()
def mongodb_extractCompany():
    companyDf = extract.extractKSQLCompany({})
    return companyDf

@task()
def transformers(stockDf, companyDf):
    df = transform.transform(stockDf, companyDf)
    return df

@task()
def mongodb_loaders(df):
    collection = "ksql-join-stock-company"
    load.insertOrUpdateCollection(collection, df)

@task()
def postgres_stock_loaders(df):
    load.add_data_sqlalchemy(df, 'ksql-stock-stream')

@task()
def postgres_company_loaders(df):
    load.add_data_sqlalchemy(df, 'ksql-company-stream')

with DAG('IDX_Stock_ETL', schedule_interval=timedelta(minutes=5), default_args=default_args, catchup=False) as dag:
    df_stock = mongodb_extractStock()
    df_company = mongodb_extractCompany()
    df_transform = transformers(df_stock, df_company)
    id = mongodb_loaders(df_transform)
    postgres_stock_loaders(df_stock)
    postgres_company_loaders(df_company)