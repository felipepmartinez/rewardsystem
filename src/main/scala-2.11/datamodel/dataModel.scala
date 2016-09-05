package datamodel

/**
  * Created by Felipe on 27/07/2016.
  */

import slick.dbio.{DBIO => _, _}
import slick.driver.H2Driver.api._
import slick.jdbc.meta.MTable
import slick.lifted.ProvenShape

import slick.dbio.Effect

import scala.concurrent.Future

object dataModel {

  case class Customer(
                       id: Int,
                       father: Int = -1,
                       score: Double = 0.0,
                       isValid: Boolean = false
                     ) {

    def hasNoFather = father == -1
    def hasFather = !hasNoFather
  }

  class CustomerTable(tag: Tag) extends Table[Customer](tag, "customers") {

    def id = column[Int]("id", O.PrimaryKey)
    def father = column[Int]("father")
    def score = column[Double]("score")
    def isValid = column[Boolean]("isValid")

    override def * : ProvenShape[Customer] = (id, father, score, isValid) <> (Customer.tupled, Customer.unapply)
    // tupled: tuple -> customer
    // unapply: customer -> tuple
  }

  lazy val Customers = TableQuery[CustomerTable]

  def getTablesAction = MTable.getTables

  val createCustomerTableAction = Customers.schema.create

  def insertCustomerAction(customers: Customer*) = Customers ++= customers.toSeq

  def insertCustomerByIdAction(id: Int) = insertCustomerAction(Customer(id = id))

  val listCustomersAction = Customers.result

  val dropDatabaseAction = Customers.schema.drop
}
