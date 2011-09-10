package orientdb.session
import scala.util.DynamicVariable
import com.orientechnologies.orient.core.db.`object`.{ ODatabaseObjectPool, ODatabaseObjectTx }
import com.orientechnologies.orient.core.command.OCommandManager;
import com.orientechnologies.orient.core.sql.OCommandExecutorSQLDelegate;
import orientdb.utils._

/**
 * Adapted from ScalaQuery session
 *
 */
object ODB {
  // NOTE: must be implemented as a Plugin for live class reloading etc.
  // see OrientDB Play! Module for that matter
  val uri = play.Play.configuration.getProperty("odb.url", "memory:test")
  val user = play.Play.configuration.getProperty("odb.user", "admin")
  val password = play.Play.configuration.getProperty("odb.password", "admin")
  
  val cmdMan = OCommandManager.instance();
  cmdMan.registerExecutor(classOf[OSQLSynchPaginatedQuery[Any]], classOf[OCommandExecutorSQLDelegate]);
  
  def openDatabase(): ODatabaseObjectTx = {
    val db: ODatabaseObjectTx = new ODatabaseObjectTx(uri)
    if (db.exists) {
      db.open(user, password)
    } else {
      db.create()
      db.getEntityManager.registerEntityClass(classOf[models.Page])
      val schema = db.getMetadata().getSchema()
      schema.createClass(classOf[models.Page]);
      schema.save()
    }
    db
  }

  def withSession[T](f: ODatabaseObjectTx => T): T = {
    val s = openDatabase()
    try { f(s) } finally s.close()
  }

  def withSession[T](f: => T): T = withSession { s: ODatabaseObjectTx => Database.dyn.withValue(s)(f) }

  def withTransaction[T](f: ODatabaseObjectTx => T): T = withSession {
    s =>
      s.begin()
      try {
        val ret: T = f(s)
        s.commit()
        ret
      } catch {
        case ex: Exception => {
          s.rollback()
          throw ex
        }
      }
  }

  def withTransaction[T](f: => T): T = withSession {
    Database.threadLocalSession.begin()
    try {
      val ret: T = f
      Database.threadLocalSession.commit()
      ret
    } catch {
      case ex: Exception => {
        Database.threadLocalSession.rollback()
        throw ex
      }
    }
  }
}

/**
 * Factory methods for creating Database objects.
 */
object Database {

  private[session] val dyn = new DynamicVariable[ODatabaseObjectTx](null)

  /**
   * An implicit function that returns the thread-local session in a withSession block
   */
  implicit def threadLocalSession: ODatabaseObjectTx = {
    val s = dyn.value
    if (s eq null)
      throw new RuntimeException("No implicit session available; threadLocalSession can only be used within a withSession block")
    else s
  }
}
  