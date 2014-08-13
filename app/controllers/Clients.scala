package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models._

object Clients extends Controller {
  val clientMap = mapping(
      "id" -> longNumber,
      "company_id" -> longNumber,
      "name" -> text,
      "password" -> text,
      "email" -> optional(text),
      "address1" -> optional(text),
      "address2" -> optional(text)
  )(Client.apply)(Client.unapply)
  val clientForm = Form(clientMap)
  
  def add(company_id:Long) = Action { implicit request => 
    clientForm.bindFromRequest.fold(
      errors => BadRequest { views.html.client(errors, routes.Clients.add(company_id)) },
      success => {
        Client.add(clientForm.bindFromRequest.get)
        Redirect(routes.Application.index)
      }
    )
  }
  def addInput(company_id:Long) = Action {
    Ok(views.html.client(
      clientForm.fill(Client(0, company_id, "", "", None, None, None)),
      routes.Clients.add(company_id)
    ))
  }
  def editInput(id:Long) = Action {
    Client.findBy(id) match {
      case Some(client) => Ok(views.html.client(clientForm.fill(client), routes.Clients.edit(id)))
      case _ => Redirect(routes.Clients.addInput(0))
    }
  }
  def edit(id:Long) = Action { implicit request => 
    clientForm.bindFromRequest.fold(
      errors => BadRequest { views.html.client(errors, routes.Clients.edit(id)) },
      success => {
        Client.save(clientForm.bindFromRequest.get)
        Redirect(routes.Application.index)
      }
    )
  }
}