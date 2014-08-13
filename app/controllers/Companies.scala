package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models._

object Companies extends Controller {
  def companyMap = mapping(
    "id" -> longNumber,
    "name1" -> text,
    "name2" -> text,
    "email" -> text,
    "address1" -> text,
    "address2" -> text,
    "cleints" -> play.api.data.Forms.seq(Clients.clientMap)
  )(Company.apply)(Company.unapply)
  def companyForm = Form(companyMap)
  
  def add = Action { implicit request => 
    companyForm.bindFromRequest.fold(
      errors => BadRequest { views.html.company(errors, routes.Companies.add) },
      success => {
        Company.add(companyForm.bindFromRequest.get)
        Redirect(routes.Application.index)
      }
    )
  }
  
  def addInput = Action {
    Ok(views.html.company(
      companyForm.fill(Company(0, "", "", "", "", "", Nil)), 
      routes.Companies.add
    ))
  }
  def editInput(id:Long) = Action {
    Company.findBy(id) match {
      case Some(company) => Ok(views.html.company(companyForm.fill(company), routes.Companies.edit(id)))
      case _ => Redirect(routes.Companies.addInput)
    }
  }
  
  def edit(id:Long) = Action { implicit request => 
    companyForm.bindFromRequest.fold(
      errors => BadRequest { views.html.company(errors, routes.Companies.edit(id)) },
      success => {
        Company.save(companyForm.bindFromRequest.get)
        Redirect(routes.Application.index)
      }
    )
  }
}