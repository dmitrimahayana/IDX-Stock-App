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
def extractStock():
    stockDf = extract.extractKSQLStock({})
    return stockDf

@task()
def extractCompany():
    companyDf = extract.extractKSQLCompany({})
    return companyDf

@task()
def transformers(stockDf, companyDf):
    df = transform.transform(stockDf, companyDf)
    return df

@task()
def loaders(df):
    collection = "ksql-join-stock-company"
    load.insertOrUpdateCollection(collection, df)

with DAG('IDX_Stock_ETL', schedule_interval=timedelta(seconds=30), default_args=default_args, catchup=False) as dag:
    extract_data1 = extractStock()
    extract_data2 = extractCompany()
    transform_data = transformers(extract_data1, extract_data2)
    id = loaders(transform_data)