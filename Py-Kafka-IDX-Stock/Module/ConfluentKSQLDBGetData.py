from ksql import KSQLAPI
from Config import config, srConfig, rootDirectory

client = KSQLAPI(srConfig['url'])