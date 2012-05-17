This is StackMob's HTTP client. To add it to your project, use this for Maven:

	```xml
	<dependency>
		<groupId>com.stackmob</groupId>
		<artifactId>newman_2.9.1</artifactId>
		<version>0.1.0-SNAPSHOT</version>
	</dependency>
	```

… or the equivalent for sbt

# Basic Usage
	
	```scala
	import com.stackmob.newman.DSL._
	import java.net.URL
	
	//execute a GET request
	val url = new URL("http://google.com")
	val response = GET(url).executeUnsafe
	println("Response returned from %s with code %d, body %s".format(url.toString,response.code,response.bodyString)
	)
	```
#The DSL
Newman comes with a DSL which is inspired by [Dispatch](http://dispatch.databinder.net/Dispatch.html), but aims to be much simpler to understand and use. This DSL is the recommended way to build requests, and the above example in "Basic Usage" uses the DSL to construct a GET request.

To start using the DSL, simply `import com.stackmob.newman.DSL._`. The methods of interest in the DSL are uppercase representations of the HTTP verbs: 

* `def GET(url: URL)(implicit client: HttpClient)`
* `def POST(url: URL)(implicit client: HttpClient)`
* `def PUT(url: URL)(implicit client: HttpClient)`
* `def DELETE(url: URL)(implicit client: HttpClient)`
* `def HEAD(url: URL)(implicit client: HttpClient)`

Notice that each method takes an implicit `HttpClient`. The `DSL` package defaults this to `ApacheHttpClient`. If you have your own implementation, simply define it as an implicit after `import`ing, or pass one explicitly.

Each method listed above returns a Builder, which works in concert with the implicit methods defined in teh `DSL` package to let you build up a request and then execute it.

# Executing Requests
Once you have an instance of `com.stackmob.newman.HttpRequest`, you'll obviously want to execute it. There are 2 methods defined on all `HttpRequest`s that execute requests differently:

* `def prepare: IO[HttpResponse]` - returns a `scalaz.effects.IO` that represents the result of executing the request. Remember that this method does not actually execute the request, and no network traffic will happen if you call this method. In order to actually execute the request, call `unsafePerformIO` on this method's result.
* `def executeUnsafe: HttpResponse` - returns the result of `prepare.unsafePerformIO`. Note that this method hits the network, and will not return until the remote server responds (ie: it's synchronous). Also, it may throw if there was a network error, etc… (hence the suffix `Unsafe`)


