package models.core

import scalikejdbc._
import scala.reflect.macros.Context
import scala.collection.mutable._
import scala.reflect.runtime.{universe => ru}
import ru._
import scala.reflect.ClassTag

/**
 * Crudのかったるいボイラーコードを駆逐する
 */
trait Crud[T] extends DbRead[T]{
  
  def mapOf(cn:ColumnName[T])(instance:T):Array[(SQLSyntax, Any)]
  /** なお、この２行はコピペする
   *  def idVal(instance:Client):Int = instance.id
   *  def support:SQLSyntaxSupport[Client] = this
   *  回避するいい策が見当たらない・・・
   *  
   *  applyはこんな感じ
   *  def apply(rn: ResultName[Client])(rs: WrappedResultSet):Client = Client(
   *  　　rs.int(rn.id),
   *  　　rs.int(rn.manager_id),
   *  　　rs.int(rn.sp_client_id)
   *  );
   *  mapOfはこんな感じ
   *   def mapOf(client:Client):Array[(SQLSyntax, Any)] = {
   *    val c = User.column
   *    Array(
   *      c.id -> user.id,
   *      c.manager_id -> user.manager_id,
   *      c.sp_client_id -> user.sp_client_id 
   *    )
   *  }
   */
  /***************************************************/
  
  /**
   * 新規追加
   */
  def add(instance:T)(implicit session: DBSession = NamedAutoSession(support.connectionPoolName)): Long = withSQL {
    insert.into(support).namedValues(mapOf(support.column)(instance): _*)
  }.updateAndReturnGeneratedKey.apply()
  
  /**
   * 編集を保存
   */
  def save(instance:T)(implicit session: DBSession = NamedAutoSession(support.connectionPoolName)):Int = {
    val c = support.column
    val byId = sqls.eq(c.id, idVal(instance))
    withSQL {
      update(support).set(mapOf(c)(instance): _*).where.append(byId)
    }.update.apply()
  }
  
  /**
   * [deleted]カラムの値を1にする
   */
  def flgDelete(id: Long)(implicit session: DBSession = NamedAutoSession(support.connectionPoolName)) = {
    val c = support.column
    val byId = sqls.eq(c.id, id)
    withSQL {
      update(support).set(c.deleted -> 1).where.append(byId)
    }.update.apply()
  }
  
  /**
   * 物理削除
   */
  def delete(id: Long)(implicit session: DBSession = NamedAutoSession(support.connectionPoolName)) = {
    val c = support.column
    val byId = sqls.eq(c.id, id)
    withSQL {
      scalikejdbc.delete.from(support).where.append(byId)
    }.update.apply()
  }
  
  def mapOfImpl[T: c.WeakTypeTag](c:Context)(cn:c.Expr[ColumnName[T]])(o:c.Expr[T]):c.Expr[Array[(SQLSyntax, Any)]] = {
    import c.universe._
    val tpe = o.actualType
    val fieldBinds = tpe.declarations.collect { 
      case field if field.isMethod && field.asMethod.isCaseAccessor => 
        val name = field.asMethod.name
        q"(cn.$name, $o.$name)"
    }
    c.Expr[Array[(SQLSyntax, Any)]] {
       q"""Array(..$fieldBinds)"""
    }
  }
}