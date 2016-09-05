
import datamodel.dataModel
import datamodel.dataModel._
import queries.queries._
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import slick.dbio.DBIO
import slick.driver.H2Driver.api._

import scala.concurrent.Future
import scala.async.Async.{async,await}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by Felipe on 30/07/2016.
  */

class RewardControllerSpec extends FunSpec with Matchers with BeforeAndAfterAll {

  val db = DatabaseConfig.forConfig[JdbcProfile]("rewarddb").db
  val rewardController = new RewardController

  private def performAction[T](action: DBIO[T]): Future[T] = db.run(action)

  private def searchById(id: Int) = performAction(selectCustomerByIdQuery(id).result)

  override protected def beforeAll(): Unit = async {
    await(performAction(dataModel.createCustomerTableAction))
  }

  override protected def afterAll(): Unit = async {
    await(performAction(dropDatabaseAction))
  }

  describe("The Reward Controller") {

    it("should register a new customer") {
      async {
        val c = await(rewardController.register(1))

        val result = await(performAction(selectCustomerByIdQuery(1).result))

        result should have length 1
        c should be(Customer(id = 1))
      }
    }

    it("should return a current customer when registering a known id") {
      async {
        val c = await(rewardController.register(1))

        val result = await(performAction(selectCustomerByIdQuery(1).result))

        result should have length 1
        c should be(Customer(id = 1))
      }
    }

    it("should check if a customer has no father correctly") {
      val c1 = Customer(id = 1)
      c1.father should be(-1)
      val c2 = Customer(id = 2, father = 1)
      c2.father should be(1)

      c1.hasNoFather should be(true)
      c2.hasNoFather should be(false)

      c1.hasFather should be(false)
      c2.hasFather should be(true)
    }

    it("should validate a customer correctly"){
      val c = Customer(id = 2)
      c.isValid should be(false)

      async {
        await(rewardController.register(c.id))
        rewardController.validate(c)

        val c2 = await(performAction(selectCustomerByIdQuery(c.id).result)).head
        c2.isValid should be(true)
      }
    }


    it("should set a customer father correctly") {
      async {
        val f = await(rewardController.register(3))
        val c = await(rewardController.register(4))

        rewardController.setFather(c, f)

        val c2 = await(performAction(selectCustomerByIdQuery(c.id).result)).head
        c2.father should be(f.id)
      }
    }

    it("should add points to a customer score") {
      async {
        val c = await(rewardController.register(5))

        c.score should be(0.0)
        rewardController.addPoints(c, 1.0)

        val c2 = await(performAction(selectCustomerByIdQuery(c.id).result)).head
        c2.score should be(1.0)
      }
    }

    it("should increase the score of every customer involved in a indication") {
      async {
        val f = await(rewardController.register(6))
        val c = await(rewardController.register(7))

        c.score should be(0.0)
        f.score should be(0.0)

        rewardController.setFather(c, f)

        val c2 = await(performAction(selectCustomerByIdQuery(c.id).result)).head

        rewardController.increaseScore(c2, 0)

        val c3 = await(performAction(selectCustomerByIdQuery(c.id).result)).head
        val f2 = await(performAction(selectCustomerByIdQuery(c.id).result)).head

        c3.score should not be (0.0)
        rewardController.father(c3) should be(f2)
        f2.score should not be (0.0)
      }
    }

    it("should process a single indication correctly") {
      rewardController.processIndication(8,9)  // 8 -> 9

      async {
        //performAction(listCustomersAction)

        val c1 = await(performAction(selectCustomerByIdQuery(8).result)).head
        val c2 = await(performAction(selectCustomerByIdQuery(9).result)).head

        c1.score should be(0.0)
        c1.isValid should be(true)

        c2.score should be(0.0)
        c2.isValid should be(false)
        c2.father should be(c1.id)
      }

    }

    it("should process a series of indications correctly") {
      async {
        await(performAction(dataModel.dropDatabaseAction))
        await(performAction(dataModel.createCustomerTableAction))

        /* Expected Input:       Expected Output
         1 2                   1:       2.5 pts
         1 3                   3:       1 pts
         3 4                   2,4,5,6: 0 pts
         2 4
         4 5
         4 6
       */

        rewardController.processIndication(1, 2)
        rewardController.processIndication(1, 3)
        rewardController.processIndication(3, 4)
        rewardController.processIndication(2, 4)
        rewardController.processIndication(4, 5)
        rewardController.processIndication(4, 6)

        val c1 = await(performAction(selectCustomerByIdQuery(1).result)).head
        val c2 = await(performAction(selectCustomerByIdQuery(2).result)).head
        val c3 = await(performAction(selectCustomerByIdQuery(3).result)).head
        val c4 = await(performAction(selectCustomerByIdQuery(4).result)).head
        val c5 = await(performAction(selectCustomerByIdQuery(5).result)).head
        val c6 = await(performAction(selectCustomerByIdQuery(6).result)).head

        c1.score should be(2.5)
        c2.score should be(0)
        c3.score should be(1)
        c4.score should be(0)
        c5.score should be(0)
        c6.score should be(0)
      }
    }



  }


}
