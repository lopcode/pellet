# Pellet

An opinionated Kotlin web framework, designed to be fast, lean, stable, and ergonomic.

This project is still in the prototyping phase, but you're welcome to pull it down and give it a try.

I wrote a blog post describing why I started Pellet, and what the design goals are: https://www.carrot.blog/posts/2021/11/building-pellet-introduction/

If you're interested, please star the project or [sponsor me](https://github.com/sponsors/CarrotCodes) ⭐️

## Examples

Starting a simple server, with one HTTP connector, and one route handler at `GET /v1/hello`, which responds with a 204:

```kotlin
fun main() = runBlocking {
    val pellet = pelletServer {
        httpConnector {
            endpoint = PelletConnector.Endpoint(
                hostname = "localhost",
                port = 8082
            )
            router {
                get("/v1/hello") {
                    HTTPRouteResponse.Builder()
                        .noContent()
                        .header("X-Hello", "World")
                        .build()
                }
            }
        }
    }
    pellet.start().join()
}
```

Pellet will start up:
```
[main] INFO dev.pellet.PelletServer - Pellet server starting...
[main] INFO dev.pellet.PelletServer - Please support development at https://www.pellet.dev/support
[main] INFO dev.pellet.PelletServer - Starting connector: HTTP(hostname=localhost, port=8082)
[main] INFO dev.pellet.PelletServer -  Routes:
[main] INFO dev.pellet.PelletServer -   PelletHTTPRoute(method=GET, uri=/v1/hello, handler=dev.pellet.DemoKt$main$1$pellet$1$1$1$1@ea4a92b)
```

Then you can hit this endpoint locally using [httpie](https://httpie.io/):
```
🥕 carrot 🗂 ~/git/pellet $ http -v localhost:8082/v1/hello
GET /v1/hello HTTP/1.1
Accept: */*
Accept-Encoding: gzip, deflate
Connection: keep-alive
Host: localhost:8082
User-Agent: HTTPie/2.6.0



HTTP/1.1 204 No Content
x-hello: World
```

You can find more examples in the `demo` subproject.

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