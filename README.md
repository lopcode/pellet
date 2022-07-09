# Pellet

[![Maven Central](https://img.shields.io/maven-central/v/dev.pellet/pellet-server?style=flat-square)](https://mvnrepository.com/artifact/dev.pellet)
[![GitHub License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat-square)](http://www.apache.org/licenses/LICENSE-2.0)
[![Sponsor](https://img.shields.io/badge/Patreon-F96854?style=flat-square&logo=patreon&logoColor=white)](https://www.patreon.com/carrotcodes)

Pellet is an opinionated Kotlin-first web framework that helps you write fast, concise, and correct backend services 🚀.

Pellet handles a huge number of requests per second, has a tiny dependency graph (`kotlin-stdlib`, `kotlinx-coroutines`, and `slf4j-api`), and offers approximately one way of doing things. The framework's conciseness is achieved through functional composition, instead of traditional JVM approaches involving annotations and reflection.

I write about building Pellet in a series on my blog: https://www.carrot.blog/series/pellet/

You can support my work by sponsoring [on Patreon](https://www.patreon.com/carrotcodes) or [GitHub Sponsors](https://www.github.com/sponsors/carrotcodes) ⭐️

## Releases

This project is still in the prototyping phase - give the latest version a try! Let me know what you think, and what to focus on next, in [GitHub Discussions](https://github.com/CarrotCodes/Pellet/discussions/categories/feedback) 💬.

Note that the prototype is built with the latest JVM LTS release at the time of writing - Java 17.

Gradle (Kotlin):
```
repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("dev.pellet:pellet-bom:0.0.13"))
    implementation("dev.pellet:pellet-server")
    implementation("dev.pellet:pellet-logging")
}
```

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

Pellet will start up, and output log messages in a structured format:
```
{"level":"info","timestamp":"2022-02-27T22:23:38.653728Z","message":"Pellet server starting...","name":"dev.pellet.server.PelletServer","thread":"main"}
{"level":"info","timestamp":"2022-02-27T22:23:38.748977Z","message":"Get help, give feedback, and support development at https://www.pellet.dev","name":"dev.pellet.server.PelletServer","thread":"main"}
{"level":"info","timestamp":"2022-02-27T22:23:38.749632Z","message":"Starting connector: HTTP(hostname=localhost, port=8082, router=dev.pellet.server.routing.http.PelletHTTPRouter@189cbd7c)","name":"dev.pellet.server.PelletServer","thread":"main"}
{"level":"info","timestamp":"2022-02-27T22:23:38.750098Z","message":"Routes: \nPelletHTTPRoute(method=GET, uri=/, handler=dev.pellet.demo.DemoKt$main$1$sharedRouter$1$1@80e75f5d)\nPelletHTTPRoute(method=POST, uri=/v1/hello, handler=dev.pellet.demo.DemoKt$main$1$sharedRouter$1$2@80e75f5d)","name":"dev.pellet.server.PelletServer","thread":"main"}
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

Which produces a request log line, including basic structured log elements like request duration and response code, like so:
```
{"level":"info","timestamp":"2022-02-27T22:23:38.762581Z","message":"127.0.0.1 - - [05/Mar/2022:23:48:27 0000] \"GET /v1/hello HTTP/1.1\" 204 0","name":"dev.pellet.server.codec.http.HTTPRequestHandler","thread":"DefaultDispatcher-worker-3","request.method":"GET","request.uri":"/v1/hello","response.code":204,"response.duration_ms":1}
```

Errors thrown in handlers are logged appropriately:
```
{"level":"error","timestamp":"2022-03-06T00:03:58.526635Z","message":"failed to handle request","name":"dev.pellet.server.codec.http.HTTPRequestHandler","thread":"DefaultDispatcher-worker-2","throwable":"java.lang.RuntimeException: intentional error\n\tat dev.pellet.demo.DemoKt.handleForceError(Demo.kt:76)\n\tat dev.pellet.demo.DemoKt.access$handleForceError(Demo.kt:1)\n\tat dev.pellet.demo.DemoKt$main$1$sharedRouter$1$3.handle(Demo.kt:19)\n\tat dev.pellet.demo.DemoKt$main$1$sharedRouter$1$3.handle(Demo.kt:19)\n\tat dev.pellet.server.codec.http.HTTPRequestHandler.handle(HTTPRequestHandler.kt:62)\n\tat dev.pellet.server.codec.http.HTTPMessageCodec.consume(HTTPMessageCodec.kt:94)\n\tat dev.pellet.server.connector.SocketConnector.readLoop(SocketConnector.kt:76)\n\tat dev.pellet.server.connector.SocketConnector.access$readLoop(SocketConnector.kt:18)\n\tat dev.pellet.server.connector.SocketConnector$readLoop$1.invokeSuspend(SocketConnector.kt)\n\tat kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)\n\tat kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:106)\n\tat kotlinx.coroutines.scheduling.CoroutineScheduler.runSafely(CoroutineScheduler.kt:571)\n\tat kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.executeTask(CoroutineScheduler.kt:750)\n\tat kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:678)\n\tat kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:665)\n"}
{"level":"info","timestamp":"2022-03-06T00:03:58.541244Z","message":"127.0.0.1 - - [06/Mar/2022:00:03:58 0000] \"GET /v1/error HTTP/1.1\" 500 0","name":"dev.pellet.server.codec.http.HTTPRequestHandler","thread":"DefaultDispatcher-worker-2","request.method":"GET","request.uri":"/v1/error","response.code":500,"response.duration_ms":2}
```

Pellet integrates nicely with `kotlinx.serialization`. For example, you can define a handler:
```kotlin
@kotlinx.serialization.Serializable
data class ResponseBody(
    val message: String
)

private suspend fun handleResponseBody(
    context: PelletHTTPRouteContext
): HTTPRouteResponse {
    val responseBody = ResponseBody(message = "hello, world 🌎")
    return HTTPRouteResponse.Builder()
        .statusCode(200)
        .jsonEntity(Json, responseBody)
        .header("X-Hello", "World")
        .build()
}
```

Which will respond like so:
```
🥕 carrot 🗂 ~/git/pellet $ http localhost:8082/v1/hello
HTTP/1.1 200 OK
Content-Length: 31
Content-Type: application/json
X-Hello: World

{
    "message": "hello, world 🌎"
}
```

Pellet supports type-safe route building and variable matching:
```kotlin
val idDescriptor = uuidDescriptor("id")
val suffixDescriptor = stringDescriptor("suffix")
val helloIdPath = PelletHTTPRoutePath.Builder()
    .addComponents("/v1")
    .addVariable(idDescriptor)
    .addComponents("/hello")
    .build()
@kotlinx.serialization.Serializable
data class ResponseBody(
    val message: String
)
router {
    get(helloIdPath) {
        val id = it.pathParameter(idDescriptor).getOrThrow()
        val suffix = it.firstQueryParameter(suffixDescriptor).getOrNull()
            ?: "👋"
        val responseBody = ResponseBody(message = "hello $id $suffix")
        return HTTPRouteResponse.Builder()
            .statusCode(200)
            .jsonEntity(Json, responseBody)
            .build()
    }
}
```

Which will respond like so:
```
🥕 carrot 🗂 ~/git/pellet $ http localhost:8082/v1/06b39add-2b57-4d58-b084-40afeacab2e9/hello
HTTP/1.1 200 OK
Content-Length: 61
Content-Type: application/json

{
    "message": "hello 06b39add-2b57-4d58-b084-40afeacab2e9 👋"
}

🥕 carrot 🗂 ~/git/pellet $ http localhost:8082/v1/06b39add-2b57-4d58-b084-40afeacab2e9/hello\?suffix=🥕
HTTP/1.1 200 OK
Content-Length: 61
Content-Type: application/json

{
    "message": "hello 06b39add-2b57-4d58-b084-40afeacab2e9 🥕"
}
```

It's easy to decode an incoming request body:
```kotlin
@kotlinx.serialization.Serializable
data class RequestBody(
    val message: String
)
@kotlinx.serialization.Serializable
data class ResponseBody(
    val message: String
)

private suspend fun handleEchoRequest(
    context: PelletHTTPRouteContext
): HTTPRouteResponse {
    val requestBody = context.decodeRequestBody<RequestBody>(Json).getOrElse {
        return HTTPRouteResponse.Builder()
            .badRequest()
            .build()
    }
    val responseBody = ResponseBody(
        message = requestBody.message
    )
    return HTTPRouteResponse.Builder()
        .jsonEntity(Json, responseBody)
        .build()
}
```

Which will echo the (well-formed) request like so:

```
🥕 carrot 🗂 ~/git/pellet $ http POST localhost:8082/v1/echo message="hello, world 🥕"
HTTP/1.1 200 OK
Content-Length: 31
Content-Type: application/json

{
    "message": "hello, world 🥕"
}
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
