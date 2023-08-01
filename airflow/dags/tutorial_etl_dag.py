from airflow import DAG
from airflow.decorators import task
from airflow.utils.dates import days_ago
import pandas
from pymongo import MongoClient

default_args = {
    'owner': 'Dmitri',
    'start_date': days_ago(1),
}

@task()
def extract():
    client = MongoClient()
    client = MongoClient("mongodb://localhost:27017/")
    mydatabase = client["kafka"]
    mycollection  = mydatabase["ksql-stock-stream"]
    data = {
        'cars': ["BMW", "Volvo", "Ford"],
        'passings': [3, 7, 2]
    }
    return data

@task()
def transform(data):
    df = pandas.DataFrame(data)
    df["newcol"] = "Hello World"
    return df

@task()
def load(df):
    return df

with DAG('tutorial_etl_dag', schedule_interval='@once', default_args=default_args, catchup=False) as dag:
    extract_data = extract()
    transform_data = transform(extract_data)
    load(transform_data)