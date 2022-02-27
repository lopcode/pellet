# Pellet

An opinionated Kotlin web framework, with best-practices built-in.

Pellet handles a huge number of requests per second, has a tiny dependency graph (`kotlin-stdlib`, `kotlinx-coroutines`, and `slf4j-api`), and offers approximately one way of doing things.

This project is still in the prototyping phase, but you're welcome to pull it down and give it a try.

I write about building Pellet in a series on my blog: https://www.carrot.blog/series/pellet/

You can support my work by sponsoring [on Patreon](https://www.patreon.com/carrotcodes) or [GitHub Sponsors](https://www.github.com/sponsors/carrotcodes) ⭐️

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
{"level":"info","timestamp":"2022-02-27T22:23:38.653728Z","message":"Pellet server starting...","name":"dev.pellet.server.PelletServer","thread":"main"}
{"level":"info","timestamp":"2022-02-27T22:23:38.748977Z","message":"Get help, give feedback, and support development at https://www.pellet.dev","name":"dev.pellet.server.PelletServer","thread":"main"}
{"level":"info","timestamp":"2022-02-27T22:23:38.749632Z","message":"Starting connector: HTTP(hostname=localhost, port=8082, router=dev.pellet.server.routing.http.PelletHTTPRouter@189cbd7c)","name":"dev.pellet.server.PelletServer","thread":"main"}
{"level":"info","timestamp":"2022-02-27T22:23:38.750098Z","message":"Routes: \nPelletHTTPRoute(method=GET, uri=/, handler=dev.pellet.demo.DemoKt$main$1$sharedRouter$1$1@80e75f5d)\nPelletHTTPRoute(method=POST, uri=/v1/hello, handler=dev.pellet.demo.DemoKt$main$1$sharedRouter$1$2@80e75f5d)","name":"dev.pellet.server.PelletServer","thread":"main"}
{"level":"info","timestamp":"2022-02-27T22:23:38.759513Z","message":"Starting connector: HTTP(hostname=localhost, port=8083, router=dev.pellet.server.routing.http.PelletHTTPRouter@189cbd7c)","name":"dev.pellet.server.PelletServer","thread":"main"}
{"level":"info","timestamp":"2022-02-27T22:23:38.759684Z","message":"Routes: \nPelletHTTPRoute(method=GET, uri=/, handler=dev.pellet.demo.DemoKt$main$1$sharedRouter$1$1@80e75f5d)\nPelletHTTPRoute(method=POST, uri=/v1/hello, handler=dev.pellet.demo.DemoKt$main$1$sharedRouter$1$2@80e75f5d)","name":"dev.pellet.server.PelletServer","thread":"main"}
{"level":"info","timestamp":"2022-02-27T22:23:38.762581Z","message":"Pellet started in 145ms","name":"dev.pellet.server.PelletServer","thread":"main"}
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
Copyright 2021-2022 CarrotCodes

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