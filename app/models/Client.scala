package models

import scalikejdbc._
import models.core.Crud

case class Client(
	id:Long, 
	company_id:Long, 
	name:String, 
	password:String, 
	email:Option[String], 
	address1:Option[String], 
	address2:Option[String])

object Client extends Crud[Client]{
  
  def apply(rn: ResultName[Client])(rs: WrappedResultSet):Client = Client(
      rs.get(rn.id),
      rs.get(rn.company_id),
      rs.get(rn.name),
      rs.get(rn.password),
      rs.get(rn.email),
      rs.get(rn.address1),
      rs.get(rn.address2)
  )
  
  def mapOf(cn:ColumnName[Client])(client:Client):Array[(SQLSyntax, Any)] = Array(
      cn.company_id -> client.company_id,
      cn.name -> client.name,
      cn.password -> client.password,
      cn.email -> client.email,
      cn.address1 -> client.address1,
      cn.address2 -> client.address2
  )
}