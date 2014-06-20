<img src="https://raw.githubusercontent.com/wiki/monifu/monifu.js/assets/monifu.png" align="right" />

Extensions to Scala's standard library for multi-threading primitives and functional reactive programming. Targets both the JVM and [Scala.js](http://www.scala-js.org/).

[![Build Status](https://travis-ci.org/monifu/monifu.js.png?branch=master)](https://travis-ci.org/monifu/monifu.js)

## Teaser

[Reactive Extensions](https://github.com/monifu/monifu.js/wiki/Reactive-Extensions-(Rx))

```scala
import monifu.concurrent.Scheduler.Implicits.global
import monifu.reactive._

// emits an auto-incremented number, every second
Observable.interval(1.second)
  // drops the first 10 emitted events
  .drop(10) 
  // takes the first 100 emitted events  
  .take(100) 
  // per second, makes requests and concatenates the results
  .flatMap(x => request(s"http://some.endpoint.com/request?tick=$x"))
  // filters only valid responses
  .filter(response => response.status == 200) 
  // processes response, selecting the body
  .map(response => response.body) 
  // creates subscription, foreach response print it
  .foreach(x => println(x)) 
```

## Documentation

The available documentation is maintained as a [GitHub's Wiki](https://github.com/monifu/monifu.js/wiki).
Work in progress.

* [Reactive Extensions (Rx)](https://github.com/monifu/monifu.js/wiki/Reactive-Extensions-%28Rx%29)
* [Atomic References](https://github.com/monifu/monifu.js/wiki/Atomic-References) 
* [Schedulers](https://github.com/monifu/monifu.js/wiki/Schedulers) 

Also see:

* [API Documentation](http://www.monifu.org/monifu.js/current/api/)
* [Reactive-Streams.org](http://www.reactive-streams.org/)

Release Notes:

* [Version 0.13 - Jun 19, 2014](https://github.com/monifu/monifu.js/wiki/0.13)
* [Version 0.12 - May 31, 2014](https://github.com/monifu/monifu.js/wiki/0.12)
* [Other Releases](https://github.com/monifu/monifu.js/wiki/Release-Notes)

## Usage

The packages are published on Maven Central.

Compiled for Scala 2.10 and 2.11 for the latest Scala.js (`0.5.0`). 

```scala
libraryDependencies += "org.monifu" %% "monifu-js" % "0.13.0"
```

## License

All code in this repository is licensed under the Apache License, Version 2.0.
See [LICENCE.txt](./LICENSE.txt).
