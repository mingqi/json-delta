# Introduction

This is simpoe json delta tools. Just like linux "diff" and "pitch". You can compare two json file and generate a .delta file. Then you can apply the .delta on the old file to get a new file.

# Delta Object

Delta is JSON object and self-contained. It's very simple and look like:

Delta Object:
```
{
    "delta_type": "<delta_type>", # REPLACE | REMOVE | CHANGE | NEW
    "delta_value" : <delta_value>, # could be any JSON value or a list of Delta object, which depend on the <delta_type>

}
```

Give you a example. We have two version JSON, version1 and version2.

version1 is:
```
{
    "firstName": "John",
    "secondName": "Smith",
    "age": 27,
    "isAlive": true,
    "address": {
        "streetAddress": "21 2nd Street",
        "city": "New York",
        "state": "NY",
        "postalCode": "10021-3100"   
    },
    "phoneNumbers": [
        {
          "type": "home",
          "number": "212 555-1234"
        },
        {
          "type": "office",
          "number": "646 555-4567"
        },
        {
          "type": "mobile",
          "number": "123 456-7890"
        }
    ]
}
```

We get version2 by making some changes on version1:
```
{
    "firstName": "John",
    "secondName": "Smith",
    "age": 30, # change value from 27 -> 30 
    # "isAlive": true, # delete this field
    "address": {
        "streetAddress": "21 2nd Street",
        "city": "New York",
        "state": "NY",
        "postalCode": "20021-3100", # change value from 10021-3100 --> 20021-3100
    },
    "phoneNumbers": [
        {
          "type": "home",
          "number": "212 555-1234"
        },
        {

            },
        {
          "type": "office",
          "number": "646 555-4567"
        }
        # Delete mobile number here
    ]
}
```


The delta file of version1 and version2 is:

```
{
    "delta_type": "CHANGE",
    "delta_value": {
        "age":{
            "delta_type": "REPLACE",
            "delta_value": 30
        },
        "isAlive"{
            "delta_type": "REMOVE",
        },
        "address"{
            "delta_type": "CHANGE",
            "delta_value": {
                "postalCode":{
                    "delta_type": "REPLACE",
                    "delta_value": "20021-3100"                
                }
            }
        }，
        "phoneNumbers": {
            "delta_type" : "CHANGE",
            "delta_value": [
                {
                    "index": 2,
                    "delta":{
                        "delta_type": "REMOVE"
                    }
                }
            ]
        }
    }
}
```


## REPLACE
Replace current JSON object by a new one. The new "delta_value" is the new object. This only happened if two compared JSON objects are:

1. primitive JSON type (number, String) 
2. or they have difference type. e.g. One JSON is List type and another one is Object JSON type.

## REMOVE
Remove a object

## CHANGE
Make some changes on current object. This only happed on **List and Object** JSON type.

## NEW
Add a new value on current object. This only happed on **List and Object** JSON type.

