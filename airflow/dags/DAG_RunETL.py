from datetime import datetime, timedelta
from airflow import DAG
from airflow.decorators import task
from airflow.operators.bash import BashOperator
import sys
# sys.path.insert(0, '/mnt/hgfs/00 My Project/')
# from Aggregate_Dataset import *


# A DAG represents a workflow, a collection of tasks
with DAG(dag_id="Amazon_ETL", start_date=datetime(2022, 10, 28), schedule=timedelta(minutes=1)) as dag:

    # Tasks are represented as operators
    prepareLogs = BashOperator(task_id="Log_Creation", bash_command="mkdir -p /home/dmahayana/airflow/dags/customLogs /home/dmahayana/airflow/dags/customLogs/touch test_$(date +'%F_%T')")

    @task()
    def RunETL():
        ProcceedOutput()
        print("airflow was successfully perform ETL")

    # Set dependencies between tasks
    prepareLogs >> RunETL()
