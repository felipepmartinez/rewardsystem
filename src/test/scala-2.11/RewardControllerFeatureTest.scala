/**
  * Created by Felipe on 26/07/2016.
  */
import com.twitter.finagle.http.{FileElement, Status}
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import org.scalatest.Matchers
import com.twitter.finatra.json.JsonDiff._
import com.twitter.io.Buf

import scala.concurrent.Await

class RewardControllerFeatureTest extends FeatureTest with Matchers {
  override val server: EmbeddedHttpServer = new EmbeddedHttpServer(
    twitterServer = new RewardServer)


  "The Reward Server" should {

    "Say Hello" in {
      server.httpGet(
        path = "/hello",
        andExpect = Status.Ok,
        withBody = "Hello!"
      )
    }

    "Respond to /create with no Customer Table created" in {
        server.httpGet("/drop")
        val response = server.httpGet("/create")


      response.statusCode should be(200)
      response.contentString should be("Customers table created!")
    }

    "Respond to /create with Customer Table created" in {
      val response = server.httpGet("/create")

      response.statusCode should be(200)
      response.contentString should be("Customers table already exists!")
    }

    "Respond to /drop with Customer Table created" in {
      val response = server.httpGet("/drop")

      response.statusCode should be(200)
      response.contentString should be("Customers table deleted!")
    }

    "Respond to /drop with no Customer Table" in {
      val response = server.httpGet("/drop")

      response.statusCode should be(200)
      response.contentString should be("No data to delete!")
    }

    "Respond to /invite 1 2" in {
      server.httpGet("/create")
      val response = server.httpGet("/invite/1/2")

      response.statusCode should be(200)
      response.contentString should be("Customer 1 invited customer 2.")
    }


    "Respond to /score after a series of specific invites" in {
      server.httpGet("/invite/1/3")
      server.httpGet("/invite/3/4")
      server.httpGet("/invite/2/4")
      server.httpGet("/invite/4/5")
      server.httpGet("/invite/4/6")

      val response = server.httpGet("/scores")

        val expectedResponse ="""
      {
        "customers":[{
        "id":1,
        "score":2.5
      },{
        "id":3,
        "score":1.0
      },{
        "id":2,
        "score":0.0
      },{
        "id":4,
        "score":0.0
      },{
        "id":5,
        "score":0.0
      },{
        "id":6,
        "score":0.0
      }]
      }"""
      jsonDiff(response.contentString, expectedResponse)
    }

    "Respond to /upload" in {
      server.httpGet("/drop")
      server.httpGet("/create")

      server.httpMultipartFormPost("/upload",
        params = Seq(
          FileElement("file",
            Buf.ByteArray.Owned("1 2\n2 3\n".getBytes()),
            Some("text/plain"),
            Some("input.txt"))
        )
      )

      val response = server.httpGet("/scores")

      val expectedResponse =
        """
          |{
          |  "customers" : [
          |    {
          |      "id" : 1,
          |      "score" : 1.0
          |    },
          |    {
          |      "id" : 2,
          |      "score" : 0.0
          |    },
          |    {
          |      "id" : 3,
          |      "score" : 0.0
          |    }
          |  ]
          |}
        """.stripMargin

      jsonDiff(response.contentString, expectedResponse)
    }
  }

}
