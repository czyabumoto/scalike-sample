package models.core

import scalikejdbc.interpolation.SQLSyntax

object SQLSyntaxExtention {

  implicit class SQLSyntaxExtention(sqls: SQLSyntax.type) {
    /**
     * eqにNoneを入れると`column` = nullになってしまうので、isNullを使う
     */
    def eqOpt(column: SQLSyntax, value: Option[Any]): SQLSyntax = value.fold {
      sqls.isNull(column)
    } { v => 
      sqls.eq(column, value)
    }
    
    /**
     * Optionだったら自動的にeqOptを使う
     */
    def autoEqOpt(column: SQLSyntax, value: Any): SQLSyntax = value match {
      case value: Option[_] => eqOpt(column, value)
      case _ => sqls.eq(column, value)
    }
    
  }
}
