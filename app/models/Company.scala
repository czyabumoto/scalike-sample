package models

import scala.reflect.macros.Context
import scala.language.experimental.macros
import scalikejdbc._
import models.core._
import scalikejdbc.config.DBs

case class Company(id:Long, name1:String, name2:String, email:String, address1:String, address2:String, clients:Seq[Client] = Nil)

object Company extends Crud[Company]{
  // def apply(rn: ResultName[Company])(rs: WrappedResultSet):Company = macro applyImpl[Company]
  // def mapOf(cn:ColumnName[Company])(o:Company):Array[(SQLSyntax, Any)] = macro mapOfImpl[Company]
  
  DBs.setupAll()
  
  def apply(rn: ResultName[Company])(rs: WrappedResultSet):Company = Company(
    rs.get(rn.id),
    rs.get(rn.name1),
    rs.get(rn.name2),
    rs.get(rn.email),
    rs.get(rn.address1),
    rs.get(rn.address2)
  )
  
  def mapOf(cn:ColumnName[Company])(o:Company):Array[(SQLSyntax, Any)] = Array(
    cn.name1 -> o.name1,
    cn.name2 -> o.name2,
    cn.email -> o.email,
    cn.address1 -> o.address1,
    cn.address2 -> o.address2
  )
}