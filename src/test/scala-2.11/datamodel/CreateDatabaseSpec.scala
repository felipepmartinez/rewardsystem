package datamodel

/**
  * Created by Felipe on 27/07/2016.
  */


import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.concurrent._
import dataModel._
import slick.dbio._

import async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global

class CreateDatabaseSpec extends FunSpec with Matchers with BeforeAndAfterAll {

  new HikariDataSource()
  val db = DatabaseConfig.forConfig[JdbcProfile]("rewarddb").db

  private def performAction[T](action: DBIO[T]): Future[T] = db.run(action)

  override protected def afterAll(): Unit = async {
    await(performAction(dropDatabaseAction))
  }

  describe("DataModel") {

    it("should start with no tables") {
      async {
        val result = await(performAction(getTablesAction)).toList

        result.size should be(0)
      }
    }

    it("should create database") {
      async {
        await(performAction(createCustomerTableAction))

        val result = await(performAction(getTablesAction)).toList
        result.size should be(1)
      }
    }

    it("should insert a customer into database") {
      async {
        val result = await(performAction(insertCustomerAction(Customer(id = 1))))
        result should be(Some(1)) //one line affected
      }
    }

    it("should insert multiple customers into database") {
      val customers = Seq(
        Customer(id = 2, father = 1),
        Customer(id = 3, father = 1),
        Customer(id = 4, father = 2)
      )

      async {
         val result = await(performAction(insertCustomerAction(customers: _*)))
        result should be(Some(3))
      }
    }

    it("should insert a new customer by its id") {
      async {
        val result = await(performAction(insertCustomerByIdAction(5)))
        result should be(Some(1))
      }
    }

    it("should list all customers in database") {
      async {
        val result = await(performAction(listCustomersAction))
        result should have length 5
      }
    }
  }

}
