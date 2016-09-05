/**
  * Created by Felipe on 26/07/2016.
  */
import com.twitter.finatra.http.request._
import com.twitter.finatra.http.fileupload.MultipartItem
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{Controller, HttpServer}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.concurrent.{Await, Future}
//import slick.jdbc.hikaricp.HikariCPJdbcDataSource

import scala.math._
import scala.concurrent.duration._
import slick.dbio.DBIO
import slick.driver.H2Driver.api._
import datamodel.dataModel._
import queries.queries._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

import scala.io.Source
import java.io.{FileOutputStream}

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global

import com.twitter.{util => twitter}
import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.util.{Failure, Success, Try}
import language.implicitConversions

import scala.concurrent.duration.Duration


object TwitterConverters {

  implicit def scalaToTwitterTry[T](t: Try[T]): twitter.Try[T] = t match {
    case Success(r) => twitter.Return(r)
    case Failure(ex) => twitter.Throw(ex)
  }

  implicit def twitterToScalaTry[T](t: twitter.Try[T]): Try[T] = t match {
    case twitter.Return(r) => Success(r)
    case twitter.Throw(ex) => Failure(ex)
  }

  implicit def scalaToTwitterFuture[T](f: Future[T])(implicit ec: ExecutionContext): twitter.Future[T] = {
    val promise = twitter.Promise[T]()
    f.onComplete(promise update _)
    promise
  }

  implicit def twitterToScalaFuture[T](f: twitter.Future[T]): Future[T] = {
    val promise = Promise[T]()
    f.respond(promise complete _)
    promise.future
  }
}

object RewardApp extends RewardServer

class RewardServer extends HttpServer {

  override protected def defaultFinatraHttpPort: String = ":8080"

  override protected def defaultHttpServerName: String = "RewardServer"

  override protected def configureHttp(router: HttpRouter): Unit = {
    router.add(new RewardController)
  }

  Duration
}

class RewardController extends Controller {

  val db = DatabaseConfig.forConfig[JdbcProfile]("rewarddb").db

  private def performAction[T](action: DBIO[T]): Future[T] = db.run(action)

  // Methods
  def register(id: Int): Future[Customer] = async {
    val result = await(performAction(selectCustomerByIdQuery(id).result))
    if (result.isEmpty) {
      await(performAction(insertCustomerByIdAction(id)))
      Customer(id)
    } else
      result.head
  }

  def isFirstIndication(c: Customer) = !c.isValid

  def validate(c: Customer): Unit = async {
    await(performAction(validateCustomerByIdQuery(c.id)))
  }

  def setFather(c: Customer, f: Customer): Unit = async {
    await(performAction(updateCustomerFatherByIdQuery(c.id, f.id)))
  }

  def father(c: Customer): Future[Customer] = async {
    await(performAction(selectCustomerByIdQuery(c.father).result)).head
  }

  def addPoints(c: Customer, points: Double): Unit = async {
    performAction(updateScoreByIdQuery(c.id, c.score + points))
  }

  def increaseScore(c: Customer, depth: Int): Unit = {
    addPoints(c, pow(0.5, depth))

    if (c.hasFather)
      async {
        increaseScore(await(father(c)), depth + 1)
      }
  }

  def processIndication(id1: Int, id2: Int): Future[Unit] =
    async {
      val c1 = await(register(id1))
      val c2 = await(register(id2))

      if (isFirstIndication(c1)) {
        validate(c1)
        if (c1.hasFather)
          increaseScore(await(father(c1)), 0)
      }

      if (c2.hasNoFather) setFather(c2, c1)
    }

  // Routes (scala Future -> twitter Future)

  def dropRoute = async {
    if (await(performAction(getTablesAction)).toList.nonEmpty) {
      await(performAction(dropDatabaseAction))

      response.ok("Customers table deleted!")
    } else
      response.ok("No data to delete!")
  }

  def createRoute = async {
    if (await(performAction(getTablesAction)).toList.isEmpty) {
      await(performAction(createCustomerTableAction))

      response.ok("Customers table created!")
    } else
      response.ok("Customers table already exists!")
  }

  def scoresRoute = async {
    val customers = await(performAction(orderCustomersQuery.result))

    val jsonResponse = "customers" ->
      customers.map { c =>
        ("id" -> c.id) ~ ("score" -> c.score)
      }

    pretty(render(jsonResponse))
  }

  get("/hello") { request: Request =>
    "Hello!"
  }

  get("/drop") { request: Request =>
    TwitterConverters.scalaToTwitterFuture(dropRoute)
  }

  get("/create") { request: Request =>
    TwitterConverters.scalaToTwitterFuture(createRoute)
  }

  get("/scores") { request: Request =>
    TwitterConverters.scalaToTwitterFuture(scoresRoute)
  }

  get("/invite/:id1/:id2") { request: Request =>
    async {
      if (await(performAction(getTablesAction)).toList.isEmpty)
        await(performAction(createCustomerTableAction))
      }

    val id1 = request.params("id1").toInt
    val id2 = request.params("id2").toInt

    Await.result(processIndication(id1,id2), Duration.Inf)

    response.ok("Customer " + id1 + " invited customer " + id2 + ".")
  }

  post("/upload") { request: Request =>
    async {
      if (await(performAction(getTablesAction)).toList.isEmpty)
        await(performAction(createCustomerTableAction))
    }

    val data = RequestUtils.multiParams(request).get("file").map(x => x.data).get // Array[Byte]

    val tmp = System.getProperty("java.io.tmpdir")
    val fileLocation = tmp + "/upload.txt"

    val fileStream = new FileOutputStream(fileLocation)
    fileStream.write(data)
    fileStream.close

    val source = Source.fromFile(fileLocation)
    for (line <- source.getLines.toArray) {
      val customers = line.split(" ")
      Await.result(processIndication(customers(0).toInt, customers(1).toInt), Duration.Inf)
    }

    //call: curl --form "file=@input.txt" http://localhost:8080/upload
  }

}