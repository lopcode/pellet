# Pellet

An opinionated Kotlin web framework, designed to be fast, lean, stable, and ergonomic.

Pellet handles a huge number of requests per second, has a tiny dependency graph (`kotlin-stdlib`, `kotlinx-coroutines`, and `slf4j-api`), and offers approximately one way of doing things.

This project is still in the prototyping phase, but you're welcome to pull it down and give it a try.

I write about building Pellet in a series on my blog: https://www.carrot.blog/series/pellet/

If you're interested, please star the repo, or [support my work](https://www.pellet.dev/support) ⭐️

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
[main] INFO dev.pellet.PelletServer - Pellet started in 16ms
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
X-Hello: World
```

You can find more examples in the `demo` subproject.

# License

This work is, unless otherwise stated, licensed under the Apache License, Version 2.0.

```
Copyright 2021 CarrotCodes

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```