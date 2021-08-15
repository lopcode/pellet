# Pellet

An experimental Kotlin web framework. I've used a lot of "web frameworks" in various languages over the years, and want to better understand the advantages and disadvantages of their choices - from the perspective of both their internal architecture and their developer-facing APIs.

The goal is to produce a functioning web framework that I'm happy to use for my own projects, using the design goals below to guide the implementation.

If you're interested, please star the project or [sponsor me](https://github.com/sponsors/CarrotCodes) to let me know it's worth spending time on ⭐️

## Design goals

These design goals are a mixture of opinions I've formed over time, and notes to myself to guide the implementation. They're likely to expand and change as I make progress!

The framework should be:

* Simple
  * Both for developers (users of the framework), and to maintain it (myself)
  * Kotlin-first - Java interop is a bonus
  * Only target the JVM - not cross-platform
  * Reduce the size of the dependency graph where possible
  * Keep the use of annotations to a minimum
* Modern
  * Target latest stable Kotlin
  * Target latest LTS Java minimum, maybe latest stable
  * Favour proven deployment methods and patterns - e.g. 12 factor app
  * Favour the use of containers
  * Use platform advancements like Coroutines, or Loom
* Opinionated / use-case driven
  * Demo/sample projects from the start, to drive implementation
  * Don't be afraid to present a single good way of doing something
  * Favour a "three layer" architecture - `api`, `domain`, and `data`
  * Make sensible choices for the basics
    * Logging (SLF4J)
    * Metrics (OpenTelemetry)
    * Database access (JDBI? - avoid ORMs)
    * Database migrations (TBD)
    * Messaging / message queues (TBD)
* Reliable
  * High unit test coverage
  * Integration tests
  * Real usage tested with sample projects
* Fast to start up, and run
  * Proven with benchmarks
  * Reflectionless

# License

This work is, unless otherwise stated, licensed under the Apache License, Version 2.0.

```
Copyright 2021 Skye Welch

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