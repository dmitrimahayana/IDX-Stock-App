import json
import os
from Config import rootDirectory

jsonPath = os.path.join(rootDirectory, 'Resource', 'kafka.stock-stream 2023-07-28.json')  # requires `import os`

# Opening JSON file
file = open(jsonPath)

# returns JSON object as
# a dictionary
data = json.load(file)
for row in data:
    print(f'id:{row["id"]} close:{row["close"]} ')
