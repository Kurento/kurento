# All objects

## release

### Request

```JSON
{
  "jsonrpc": "2.0",
  "id": 1,

  "method": "release",
  "params":
  {
    "object":
    {
      "id": 1234,
      "token": "asdf"
    }
  }
}
```

### Response

```JSON
{
  "jsonrpc": "2.0",
  "id": 1,

  "result": null
}
```


## subscribeEvent

### Request

```JSON
{
  "jsonrpc": "2.0",
  "id": 1,

  "method": "subscribeEvent",
  "params":
  {
    "object":
    {
      "id": 1234,
      "token": "asdf"
    },
    "type": "close"
  }
}
```

### Response

```JSON
{
  "jsonrpc": "2.0",
  "id": 1,

  "result": "zxcv"
}
```


### onEvent

```JSON
{
  "jsonrpc": "2.0",

  "method": "onEvent",
  "params":
  {
    "object":
    {
      "id": 1234,
      "token": "asdf"
    },
    "type": "close",
    "data": *
  }
}
```


## unsubscribeEvent

### Request

```JSON
{
  "jsonrpc": "2.0",
  "id": 1,

  "method": "unsubscribeEvent",
  "params":
  {
    "object":
    {
      "id": 1234,
      "token": "asdf"
    },
    "token": "zxcv"
  }
}
```

### Response

```JSON
{
  "jsonrpc": "2.0",
  "id": 1,

  "result": null
}
```


## subscribeError

### Request

```JSON
{
  "jsonrpc": "2.0",
  "id": 1,

  "method": "subscribeError",
  "params":
  {
    "object":
    {
      "id": 1234,
      "token": "asdf"
    }
  }
}
```

### Response

```JSON
{
  "jsonrpc": "2.0",
  "id": 1,

  "result": "zxcv"
}
```


### onError

```JSON
{
  "jsonrpc": "2.0",

  "method": "onError",
  "params":
  {
    "object":
    {
      "id": 1234,
      "token": "asdf"
    },
    "error":
    {
      "message": "an error has occured",
      "code": 666
    }
  }
}
```


## unsubscribeError

### Request

```JSON
{
  "jsonrpc": "2.0",
  "id": 1,

  "method": "unsubscribeError",
  "params":
  {
    "object":
    {
      "id": 1234,
      "token": "asdf"
    },
    "token": "zxcv"
  }
}
```

### Response

```JSON
{
  "jsonrpc": "2.0",
  "id": 1,

  "result": null
}
```


## invoke

### Request

```JSON
{
  "jsonrpc": "2.0",
  "id": 1,

  "method": "invoke",
  "params":
  {
    "object":
    {
      "id": 1234,
      "token": "asdf"
    },
    "method": "zxcv",
    "params": {}
  }
}
```

### Response

```JSON
{
  "jsonrpc": "2.0",
  "id": 1,

  "result":
  {
    "data": "$%&/"
  }
}
```


# MediaPipeline

## createMediaPipeline

### Request

```JSON
{
  "jsonrpc": "2.0",
  "id": 1,

  "method": "createMediaPipeline"
}
```

```JSON
{
  "jsonrpc": "2.0",
  "id": 1,

  "method": "createMediaPipeline",
  "params":
  {
    "params": {}
  }
}
```

### Response

```JSON
{
  "jsonrpc": "2.0",
  "id": 1,

  "result":
  {
    "id": 1234,
    "token": "asdf"
  }
}
```


## createMediaElement

### Request

```JSON
{
  "jsonrpc": "2.0",
  "id": 2,

  "method": "createMediaElement",
  "params":
  {
    "object":
    {
      "id": 1234,
      "token": "asdf"
    },
    "type": "JackVaderFilter"
  }
}
```

```JSON
{
  "jsonrpc": "2.0",
  "id": 2,

  "method": "createMediaElement",
  "params":
  {
    "object":
    {
      "id": 1234,
      "token": "asdf"
    },
    "type": "JackVaderFilter",
    "params": {}
  }
}
```

### Response

```JSON
{
  "jsonrpc": "2.0",
  "id": 2,

  "result":
  {
    "id": 5678,
    "token": "qwer",
    "type": "JackVaderFilter"
  }
}
```


## createMediaMixer

### Request

```JSON
{
  "jsonrpc": "2.0",
  "id": 2,

  "method": "createMediaMixer",
  "params":
  {
    "pipeline":
    {
      "id": 1234,
      "token": "asdf"
    },
    "type": "mixer"
  }
}
```

```JSON
{
  "jsonrpc": "2.0",
  "id": 2,

  "method": "createMediaMixer",
  "params":
  {
    "pipeline":
    {
      "id": 1234,
      "token": "asdf"
    },
    "type": "mixer",
    "params": {}
  }
}
```

### Response

```JSON
{
  "jsonrpc": "2.0",
  "id": 2,

  "result":
  {
    "id": 5678,
    "token": "qwer",
    "type": "mixer"
  }
}
```


# MediaElement

## connect

### Request

```JSON
{
  "jsonrpc": "2.0",
  "id": 2,

  "method": "connectElements",
  "params":
  {
    "source":
    {
      "id": 1234,
      "token": "asdf"
    },
    "sink":
    {
      "id": 5678,
      "token": "qwer"
    }
  }
}
```

```JSON
{
  "jsonrpc": "2.0",
  "id": 2,

  "method": "connectElements",
  "params":
  {
    "source":
    {
      "id": 1234,
      "token": "asdf"
    },
    "sink":
    {
      "id": 5678,
      "token": "qwer"
    },
    "type": "mixer"
  }
}
```

```JSON
{
  "jsonrpc": "2.0",
  "id": 2,

  "method": "connectElements",
  "params":
  {
    "object":
    {
      "id": 1234,
      "token": "asdf"
    },
    "sink":
    {
      "id": 5678,
      "token": "qwer"
    },
    "type": "mixer",
    "description": "asdf jkl"
  }
}
```

### Response

```JSON
{
  "jsonrpc": "2.0",
  "id": 2,

  "result": null
}
```


# MediaPad

## connect

### Request

```JSON
{
  "jsonrpc": "2.0",
  "id": 2,

  "method": "connect",
  "params":
  {
    "source":
    {
      "id": 1234,
      "token": "asdf"
    },
    "sink":
    {
      "id": 5678,
      "token": "qwer"
    }
  }
}
```

### Response

```JSON
{
  "jsonrpc": "2.0",
  "id": 2,

  "result": null
}
```


## disconnect

### Request

```JSON
{
  "jsonrpc": "2.0",
  "id": 2,

  "method": "disconnect",
  "params":
  {
    "source":
    {
      "id": 1234,
      "token": "asdf"
    },
    "sink":
    {
      "id": 5678,
      "token": "qwer"
    }
  }
}
```

### Response

```JSON
{
  "jsonrpc": "2.0",
  "id": 2,

  "result": null
}
```



# MediaMixer

## createMixerEndPoint

### Request

```JSON
{
  "jsonrpc": "2.0",
  "id": 2,

  "method": "createMixerEndPoint",
  "params":
  {
    "mixer":
    {
      "id": 1234,
      "token": "asdf"
    }
  }
}
```

```JSON
{
  "jsonrpc": "2.0",
  "id": 2,

  "method": "createMixerEndPoint",
  "params":
  {
    "mixer":
    {
      "id": 1234,
      "token": "asdf"
    },
    "params": {}
  }
}
```

### Response

```JSON
{
  "jsonrpc": "2.0",
  "id": 2,

  "result":
  {
    "id": 9876,
    "token": "poiu"
  }
}
```