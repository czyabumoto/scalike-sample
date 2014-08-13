package controllers

import play.api._
import play.api.mvc._
import models._
import models.core.Crud
import models.core.DbRead

object Application extends Controller {

  def index = Action {
    val other = Client.support.asInstanceOf[DbRead[Client]]
    Ok(views.html.index {
      Company.findAllWithChildren[Client]()(other, (cm, cl) => cm.copy(clients = cl))
    })
  }
  
  def add = TODO
  def edit(id:Long) = TODO

}