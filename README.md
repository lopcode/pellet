# Pellet

An experimental Kotlin web framework, designed to be fast, lean, stable, and ergonomic.

I wrote a blog post describing why I started this project, and what the design goals are: https://www.carrot.blog/posts/2021/11/building-pellet-introduction/

If you're interested, please star the project or [sponsor me](https://github.com/sponsors/CarrotCodes) to let me know it's worth spending time on ⭐️

## Demo

You can run the program in `Demo.kt` to start two connectors by default:
```
[main] INFO dev.pellet.PelletServer - Pellet server starting...
[main] INFO dev.pellet.PelletServer - Please support development at https://www.pellet.dev/support
[main] INFO dev.pellet.PelletServer - Starting connectors:
[main] INFO dev.pellet.PelletServer -   HTTP(hostname=localhost, port=8082)
[main] INFO dev.pellet.PelletServer -   HTTP(hostname=localhost, port=8083)
```

Then you can send requests with a tool like [httpie](https://httpie.io/):
```
$ http -v localhost:8082/                 
GET / HTTP/1.1
Accept: */*
Accept-Encoding: gzip, deflate
Connection: keep-alive
Host: localhost:8082
User-Agent: HTTPie/2.6.0



HTTP/1.1 204 No Content

```

Or run load tests locally using [hey](https://github.com/rakyll/hey):
```
$ hey -z 20s http://localhost:8083

Summary:
  Total:	20.0007 secs
  Slowest:	0.0903 secs
  Fastest:	0.0000 secs
  Average:	0.0010 secs
  Requests/sec:	103331.5273
  
...
```

# License

This work is, unless otherwise stated, licensed under the Apache License, Version 2.0.

```
Copyright 2021 CarrotCodes

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```