from confluent_kafka.avro.cached_schema_registry_client import CachedSchemaRegistryClient
from confluent_kafka.schema_registry import SchemaRegistryClient as confluentSchemaClient
from Config import srConfig

cacheSchema = CachedSchemaRegistryClient(srConfig)
stockSchema = cacheSchema.get_latest_schema("/IDX-Stock")[1]
companySchema = cacheSchema.get_latest_schema("/IDX-Company")[1]
print(stockSchema)
print(companySchema)

schema_registry_client = confluentSchemaClient({'url': "http://localhost:8282"})
mySchema = schema_registry_client.get_schema(9)
print(mySchema.schema_str)