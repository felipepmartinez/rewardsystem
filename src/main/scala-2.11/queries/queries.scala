package queries

/**
  * Created by Felipe on 28/07/2016.
  */

import datamodel.dataModel._
import slick.driver.H2Driver.api._

import scala.concurrent.Future

object queries {

  val selectAllCustomersQuery = Customers
  val selectAllCustomersIdQuery = Customers.map(_.id) // Customers.map(c => c.id)
  val selectAllValidCustomersQuery = Customers.filter(_.isValid)
  val orderCustomersQuery = Customers.sortBy(_.score desc)

  def selectCustomerByIdQuery(id:Int) = Customers.filter(_.id === id)
  def updateScoreByIdQuery(id:Int, newScore:Double) = Customers.filter(_.id === id).map(_.score).update(newScore)
  def validateCustomerByIdQuery(id:Int) = Customers.filter(_.id === id).map(_.isValid).update(true)
  def updateCustomerFatherByIdQuery(id:Int, newFather:Int) = Customers.filter(_.id === id).map(_.father).update(newFather)
}
