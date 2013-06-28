#Newman

[![Build Status](https://travis-ci.org/stackmob/newman.png?branch=master)](https://travis-ci.org/stackmob/newman)

Newman is StackMob's HTTP client. We named it after this man:

![Newman](https://a248.e.akamai.net/camo.github.com/e39710b58661110e1c932cc9c76e0dd4e9abae43/687474703a2f2f756e6465727374616e64686973746f72796e6f772e66696c65732e776f726470726573732e636f6d2f323031322f30352f6e65776d616e2e6a7067)

And wrote a post explaining our motivation for building this library [here](https://blog.stackmob.com/2013/03/newman/).

Newman supports the following basic features:

* Making HTTP requests and receiving responses
* Serializing and deserializing request and response bodies
* Serializing and deserializing requests and responses for replay or caching
* In memory response caching with TTL expiry
* ETag HTTP caching

To add it to your project, use this for Maven:

```xml
<dependency>
  <groupId>com.stackmob</groupId>
  <artifactId>newman_${scala.version}</artifactId>
  <version>0.20.0</version>
</dependency>
```

or the equivalent for sbt:

```scala
libraryDependencies += "com.stackmob" %% "newman" % "0.20.0"
```

# Basic Usage
	
```scala
import com.stackmob.newman._
import com.stackmob.newman.dsl._
import java.net.URL

implicit val httpClient = new ApacheHttpClient
//execute a GET request
val url = new URL("http://google.com")
val response = GET(url).executeUnsafe
println("Response returned from %s with code %d, body %s".format(url.toString,response.code,response.bodyString))
```

#The DSL
Newman comes with a DSL which is inspired by [Dispatch](http://dispatch.databinder.net/Dispatch.html), 
but uses mostly english instead of symbols.
This DSL is the recommended way to build requests, and the above example in "Basic Usage" uses the DSL to 
construct a GET request.

To start using the DSL, simply `import com.stackmob.newman.dsl._`. 
The functions of interest in the DSL are uppercase representations of the HTTP verbs: 

* `def GET(url: URL)(implicit client: HttpClient)`
* `def POST(url: URL)(implicit client: HttpClient)`
* `def PUT(url: URL)(implicit client: HttpClient)`
* `def DELETE(url: URL)(implicit client: HttpClient)`
* `def HEAD(url: URL)(implicit client: HttpClient)`

Notice that each method takes an implicit `HttpClient`, so you must declare your own implicit before 
you use any of the above listed DSL methods, or pass one explicitly.

Each method listed above returns a Builder, which works in concert with the implicit methods defined 
in the `DSL` package to let you build up a request and then execute it.

# Executing Requests
Once you have an instance of `com.stackmob.newman.HttpRequest`, you'll obviously want to execute it. There are 2 methods defined on all `HttpRequest`s that execute requests differently:

* `def prepare: IO[HttpResponse]` - returns a `scalaz.effects.IO` that represents the result of executing the request. Remember that this method does not actually execute the request, and no network traffic will happen if you call this method. In order to actually execute the request, call `unsafePerformIO()` on this method's result.
* `def executeUnsafe: HttpResponse` - returns the result of `prepare.unsafePerformIO()`. Note that this method hits the network, and will not return until the remote server responds (ie: it's synchronous). Also, it may throw if there was a network error, etc (hence the suffix `Unsafe`)

# Serializing
Newman comes with built in support for serializing `HttpRequest`s and `HttpResponse`s to Json.

To serialize either, simply call the `toJson(prettyPrint: Boolean = false): String` method on the `HttpRequest` or `HttpResponse`. And to deserialize, call `HttpRequest.fromJson(json: String): Result[HttpRequest]` or `HttpResponse.fromJson(json: String): Result[HttpResponse]` to deserialize the `HttpRequest` or `HttpResponse`, respectively.

# ETag Support
Newman comes with an implementation of `HttpClient` called `ETagAwareHttpClient`. This implementation requires an underlying "raw" `HttpClient` to execute requests to a server, but it also requires an implementation of `HttpResponseCacher`.

It uses this `HttpResponseCacher` to check the cache for a response corresponding to a given request. If it finds one and that response has an `ETag` header in it, the `ETagAwareHttpClient` automatically sends an `If-None-Match` header to the server containing that `ETag`. In this case, if the server responds with a `304 NOT MODIFIED` response code, then `ETagAwareHttpClient` will return the cached version. In all other cases, `ETagAwareHttpClient` will cache and return the new response.

## Usage With the DSL
Using `ETagAwareHttpClient` is very similar to the basic usage above. Following demonstrates how to use the client with a (built-in) in-memory cache implementation.

```scala
import com.stackmob.newman.{ETagAwareHttpClient, ApacheHttpClient}
import com.stacmob.newman.caching.InMemoryHttpResponseCacher
import com.stackmob.newman.DSL._
import java.net.URL
	
//change this implementation to your own if you want to use Memcached, Redis, etc
val cache = new InMemoryHttpResponseCacher
val rawHttpClient = new ApacheHttpClient
//eTagClient will be used in the DSL to construct & execute requests below
implicit val eTagClient = new ETagAwareHttpClient(rawHttpClient, cache)
	
val url = new URL("http://stackmob.com")
//since the cacher is empty, this will issue a request to stackmob.com without an If-None-Match header
val res1 = GET(url) executeUnsafe
//assuming res1 contained an ETag and stackmob.com fully supports ETag headers,
//stackmob.com will return a 304 response code in this request and res2 will come from the cache
val res2 = GET(url) executeUnsafe
```
