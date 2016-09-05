package queries

import datamodel.dataModel
import datamodel.dataModel.Customer
import queries._
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import slick.backend.DatabaseConfig
import slick.dbio.DBIO
import slick.driver.H2Driver.api._
import slick.driver.JdbcProfile

import scala.concurrent._
import scala.async.Async.{async,await}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by Felipe on 28/07/2016.
  */
class QueriesSpec extends FunSpec with Matchers with BeforeAndAfterAll {

  val db = DatabaseConfig.forConfig[JdbcProfile]("rewarddb").db

  val c1 = Customer(id = 1, score = 10, isValid = true)
  val c2 = Customer(id = 2, father = 1, score = 5, isValid = true)
  val c3 = Customer(id = 3, father = 1, isValid = true)
  val c4 = Customer(id = 4, father = 2)

  private def performAction[T](action: DBIO[T]): Future[T] = db.run(action)

  override protected def beforeAll(): Unit = async {
    await(performAction(dataModel.createCustomerTableAction))

    val customers = Seq(c1,c2,c3,c4)
    await(performAction(dataModel.insertCustomerAction(customers: _*)))
  }

  override protected def afterAll(): Unit = async {
    await(performAction(dataModel.dropDatabaseAction))
  }

  describe("Queries") {

    it("should select all customers in the database") {
      async {
        val customers = await(performAction(selectAllCustomersQuery.result)) //transform query in an action -> .result

        customers should have length 4
        customers should contain theSameElementsAs (Seq(c1, c2, c3, c4))
      }
    }

    it("should select all customers id in the database") {
      async {
        val customers = await(performAction(selectAllCustomersIdQuery.result))

        customers should have length 4
        customers should be(Seq(1, 2, 3, 4))
      }
    }

     it("should select only the valid customers") {
       async {
         val customers = await(performAction(selectAllValidCustomersQuery.result))

         customers should have length 3
         customers should contain only(c1, c2, c3)
       }
     }

    it("should list customers ordered by score") {
      async {
        val customers = await(performAction(orderCustomersQuery.result))

        customers should have length 4
        customers should contain inOrder(c1, c2)
      }
    }

    it("should return a customer searching for its id") {
      async {
        val customers = await(performAction(selectCustomerByIdQuery(1).result))

        customers should have length 1
        customers should contain only (c1)
      }
    }

    it("should not return a customer when shearching by the wrong id") {
      async {
        val customers = await(performAction(selectCustomerByIdQuery(-1).result))
        customers should have length 0
      }
    }

    it("should update the score of a customer") {
      async {
        val result = await(performAction(updateScoreByIdQuery(1, 2)))
        result should be(1) // one line affected

        val customer = await(performAction(selectCustomerByIdQuery(1).result))

        customer should have length 1
        customer.head.score should be(2)
      }
    }

    it("should validate a customer by its id") {
      async {
        await(performAction(validateCustomerByIdQuery(4)))

        val customers = await(performAction(selectAllValidCustomersQuery.result))
        customers should have length 4
      }
    }

    it("should update a customer inviter using ids") {
      async {
        val result = await(performAction(updateCustomerFatherByIdQuery(4, 3)))
        result should be(1)

        val customer = await(performAction(selectCustomerByIdQuery(4).result))
        customer.head.father should be(3)
      }
    }

  }

}
