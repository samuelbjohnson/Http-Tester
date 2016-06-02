import com.ning.http.client.Realm.{AuthScheme, RealmBuilder}
import com.ning.http.client.{AsyncCompletionHandler, AsyncHttpClient, AsyncHttpClientConfig, Response}

import scala.concurrent.{Await, Promise}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

case class Output(body: String, statusCode: Int, message: String, responseType: String)

object Tester extends App {
	if (args.length != 3) {
		System.err.println("Proper usage is java -jar Tester.jar <url> <username> <password>")
	} else {
		val url = args(0)
		val username = args(1)
		val password = args(2)
		println(s"Testing GET $url with credentials(username: $username, password of length: ${password.length})")

		val config = new AsyncHttpClientConfig.Builder()

		val realm = new RealmBuilder()
			.setPrincipal(username)
			.setPassword(password)
			.setUsePreemptiveAuth(true)
			.setScheme(AuthScheme.BASIC)
			.build()
		config setRealm realm

		val client = new AsyncHttpClient(config.build())

		val request = client.prepareGet(url).build()
		var result = Promise[Output]()
		client.executeRequest(request, new AsyncCompletionHandler[Response]() {
			override def onCompleted(response: Response) = {
				result.success(Output(response.getResponseBody, response.getStatusCode, response.getStatusText, response.getContentType))
				response
			}

			override def onThrowable(t: Throwable) {
				result.failure(t)
			}
		})
		val getFuture = result.future

		getFuture.map { result =>
			println(
				s"""Response from server:
					 |Status: ${result.statusCode}:${result.message}
					 |Content-Type: ${result.responseType}
					 |Body: ${result.body}
				 """.stripMargin)
		}.recover {
			case e: Throwable =>
				System.err.println(s"Exception thrown: $e")
		}
		Await.result(getFuture, 10 seconds)
		client.close()
	}

}