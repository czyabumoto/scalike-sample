package models.core

import scalikejdbc._
import scala.reflect.macros.Context

/**
 * 読み取り専用テーブルのためのクラス
 */
trait DbRead[T] extends SQLSyntaxSupport[T]{
  /** これだけ子クラスでの実装が必要。できればマクロ化したい。。 **/
  def idVal(instance:T):Long = cls.getField("id").get(instance).asInstanceOf[Long]
  // 子クラスで「 = this」オーバーライドしたらおしまい
  def support:SQLSyntaxSupport[T] = companionCls.getField("MODULE$").get(null).asInstanceOf[SQLSyntaxSupport[T]]
  // デフォルトでキャメルケースに変換
  def objectName:String = "models." + tableName.capitalize.iterator.map(_.toString).reduce{ (ch, c) => ch match {
        case str if (str.endsWith("_")) => str.dropRight(1) + c.toUpperCase
        case str => str + c
      }
    }
  // javaのクラスオブジェクトから取得
  def cls:Class[_] = Class.forName(objectName)
  def companionCls:Class[_] = Class.forName(objectName + "$")
  def apply(rn: ResultName[T])(rs: WrappedResultSet):T
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
   */
  
  def x = support.syntax
  def apply(cx: SyntaxProvider[T])(rs:WrappedResultSet):T = apply(cx.resultName )(rs)
  def opt(cx: SyntaxProvider[T])(rs: WrappedResultSet) = rs.longOpt(cx.resultName.id).map(_ => apply(cx)(rs))
  def parent_id[P](parent:DbRead[P]) = x.selectDynamic("%s_id".format(parent.tableName))
  
  private def head(list:List[T]) = if (list.isEmpty) None else list.head
    /**
   * 条件検索　※ = 検索
   */
  def findAll(condition:(Symbol, Any)*)(implicit session: DBSession = NamedAutoSession(support.connectionPoolName)): List[T] = {
    val cn = support.column
    // SQLで、= "value"
    val sqlParams = condition.map { case(sym, cond) => Some(sqls.eq(cn.column(sym.name), cond)) }
    
    withSQL {
      select.from(support as x).where(sqls.toAndConditionOpt(sqlParams: _*))
    }.map(apply(x)(_)).list.apply()
  }
  
  /**
   * 条件検索　※LIKE検索
   */
  def searchAll(condition:(Symbol, Any)*)(implicit session: DBSession = NamedAutoSession(support.connectionPoolName)): List[T] = {
    val cn = support.column
    // SQLで、LIKE "%value%"
    val sqlParams = condition.map { case(sym, cond) => Some(sqls.like(cn.column(sym.name), "%%%s%%".format(cond))) }
    
    withSQL {
      select.from(support as x).where(sqls.toAndConditionOpt(sqlParams: _*))
    }.map(apply(x)(_)).list.apply()
  }
  
  /**
   * 条件検索
   */
  def findAllWhere(condition: ColumnName[T] => Option[SQLSyntax])(implicit session: DBSession = NamedAutoSession(support.connectionPoolName)): List[T] = withSQL {
    condition(support.column) match { 
      case Some(c) => select.from[T](support as x).where(c)
      case _ => select.from[T](support as x)
    }
  }.map(apply(x)(_)).list.apply()
  
  /**
   * 条件指定で１件取得　※==で判定
   */
  def find(condition:(Symbol, Any)*)(implicit session: DBSession = NamedAutoSession(support.connectionPoolName)): Option[T] = {
    val cn = support.column
    // SQLで、LIKE "%value%"
    val sqlParams = condition.map { case(sym, cond) => Some(sqls.eq(cn.column(sym.name), cond)) }
    
    withSQL {
      select.from(support as x).where(sqls.toAndConditionOpt(sqlParams: _*)).limit(1)
    }.map(apply(x)(_)).first.apply()
  }
  
  /**
   * 条件指定で１件取得
   */
  def findWhere(condition: ColumnName[T] => Option[SQLSyntax])(implicit session: DBSession = NamedAutoSession(support.connectionPoolName)): Option[T] = withSQL {
    condition(support.column) match { 
      case Some(c) => select.from[T](support as x).where(c).limit(1)
      case _ => select.from[T](support as x).limit(1)
    }
  }.map(apply(x)(_)).first.apply()
  
  /**
   * IDから一件取得
   */
  def findBy(id: Long)(implicit session: DBSession = NamedAutoSession(support.connectionPoolName)): Option[T] = withSQL {
    select.from(support as x).where.eq(x.id, id)
  }.map(this.apply(x)(_)).first.apply()
  
  /**
   * One-to-OneのJOIN
   */
  def findWithBy[U](id:Long, other:DbRead[U], binder:(T,U) => T)(implicit session: DBSession = NamedAutoSession(support.connectionPoolName)): Option[T] = withSQL {
    select.from(support as x)
    .leftJoin(other.support as other.syntax).on(other.parent_id(this), x.id)
    .where.eq(x.id, id)
    .limit(1)
  }.one(apply(x)(_))
  .toOptionalOne(other.opt(other.syntax)(_))
  .map { (base, joined) => binder(base, joined) }
  .first.apply()
  
  /**
   * one-to-oneで全件
   */
  def findAllWith[U](condition:(Symbol, Any)*)(other:DbRead[U])(binder:(T,U) => T)(implicit session: DBSession = NamedAutoSession(support.connectionPoolName)): List[T] = {
    val cn = support.column
    val ocn = other.column
    // SQLで、= "value"
    val sqlParams = condition.map { 
      case(sym, cond) if (cn.columnNames.indexOf(sym.name) >= 0) => Some(sqls.eq(cn.column(sym.name), cond)) 
      case(sym, cond) if (ocn.columnNames.indexOf(sym.name) >= 0) => Some(sqls.eq(ocn.column(sym.name), cond)) 
    }
    
    withSQL {
      select.from(support as x)
      .leftJoin(other.support as other.x).on(other.parent_id(this), x.selectDynamic("id"))
      .where(sqls.toAndConditionOpt(sqlParams: _*))
    }.one(apply(x)(_))
    .toOptionalOne(other.opt(other.x)(_))
    .map { (base, joined) => binder(base, joined) }
    .list.apply()
  }
  
  /**
   * One-to-Mainで１件取得
   */
  def findWithChildrenBy[U](id:Long, other:DbRead[U])(binder:(T,Seq[U]) => T)(implicit session: DBSession = NamedAutoSession(support.connectionPoolName)): List[T] = withSQL {
    select.from(support as x)
    .leftJoin(other.support as other.syntax).on(other.parent_id(this), x.id)
    .where.eq(x.id, id)
    .limit(1)
  }.one(apply(x)(_))
  .toMany(other.opt(other.syntax)(_))
  .map { (base, joined) => binder(base, joined) }
  .list.apply()
  
  /**
   * one-to-manyで全件
   */
  def findAllWithChildren[U](condition:(Symbol, Any)*)(other:DbRead[U], binder:(T,Seq[U]) => T)(implicit session: DBSession = NamedAutoSession(support.connectionPoolName)): List[T] = {
    val cn = support.column
    val ocn = other.column
    // SQLで、= "value"
    val sqlParams = condition.map { 
      case(sym, cond) if (cn.columnNames.indexOf(sym.name) >= 0) => Some(sqls.eq(cn.column(sym.name), cond)) 
      case(sym, cond) if (ocn.columnNames.indexOf(sym.name) >= 0) => Some(sqls.eq(ocn.column(sym.name), cond)) 
    }
    
    withSQL {
      select.from(support as x)
      .leftJoin(other.support as other.x).on(other.parent_id(this), x.selectDynamic("id"))
      .where(sqls.toAndConditionOpt(sqlParams: _*))
    }.one(apply(x)(_))
    .toMany(other.opt(other.x)(_))
    .map { (base, joined) => binder(base, joined) }
    .list.apply()
  }
  
  /**
   * applyのマクロ実装
   */
  def applyImpl[T: c.WeakTypeTag](c:Context)(rn: c.Expr[ResultName[T]])(rs: c.Expr[WrappedResultSet]):c.Expr[T] = {
    import c.universe._
    val tpe = weakTypeOf[T]
    val fieldBinds = tpe.declarations.collect { 
      case field if field.isMethod && field.asMethod.isCaseAccessor => 
        val name = field.asMethod.name
        q"($rs.$name.get($rn.$name))"
    }
    c.Expr[T] {
       q"""$tpe(..$fieldBinds)"""
    }
  }
}