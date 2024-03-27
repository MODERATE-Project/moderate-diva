from paho.mqtt.publish import single

import time
import json
import certifi

try:
    while True:
        message = {
            "timestamp": str(time.time_ns()//1000),
            "sourceID": "1", 
            "sourceType": "ciao", 
            "metricTypeID": "ok", 
            "infoType": "", 
            "dataType": "Map", 
            "dataItemID": "moderate/lombardia", 
            "metricValue": {
                "ciao": 1
            }, 
            "measureUnit": "Map"
        }
        
        single(
            hostname="maestri.ismb.it",
            port=8883,
            topic="moderate/lombardia",
            payload=json.dumps(message),
            tls={'ca_certs': certifi.where()},
            auth={
                'username': "maestri",
                'password': "maestri"
            }
        )
        
        #print("Message SENT")
        time.sleep(0.001)
        
except KeyboardInterrupt:
    pass