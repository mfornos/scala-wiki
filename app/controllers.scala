package controllers

import play._
import play.mvc._
import play.mvc.results._
import play.data.validation._
import play.utils._
import orientdb.session._
import models._
import orientdb.utils._
import com.orientechnologies.orient.core.db.`object`.ODatabaseObjectTx
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery
import com.orientechnologies.orient.core.id._
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._

object Application extends Controller {
  import views.Application._
  val pageSize: Int = play.Play.configuration.getProperty("wiki.page.size", "10").toInt

  implicit def dbWrapper(s: ODatabaseObjectTx) = new {
    def queryBySql[T](sql: String, params: AnyRef*): List[T] = { queryByPageSql[T](sql, 0, -1, params: _*) }

    def queryByPageSql[T](sql: String, offset: Int, limit: Int, params: AnyRef*): List[T] = {
      val params4java = params.toArray
      val results: java.util.List[T] = s.query(new OSQLSynchPaginatedQuery[T](sql, offset, limit), params4java: _*)
      results.asScala.toList
    }
  }

  def index = {
    val cp = if (params.get("cp") == null) { 0 } else { scala.math.max(params.get("cp").toInt - 1, 0) }
    ODB withSession {
      s: ODatabaseObjectTx =>
        val pages = s.queryByPageSql[Page]("select from Page", cp * pageSize, pageSize)
        val total = s.countClass(classOf[Page])
        html.index("Play! Scala Wiki", pages, total, pageSize, cp + 1)
    }
  }

  def edit(title: String) = {
    val pages = pageByTitle(title)
    html.create(title, if (pages.isEmpty) new Page() else pages.get(0))
  }

  def save() = {
    val id = params.get("id")
    val page: Page = {
      ODB withSession { s: ODatabaseObjectTx => if (id.length == 0) new Page() else s.load(new ORecordId(id)) }
    }
    page.title = params.get("title")
    page.content = params.get("content")
    page.description = params.get("description")
    Validation.valid("Page", page)
    if (Validation.hasErrors) {
      html.create(page.title, page)
    } else {
      ODB withTransaction { s: ODatabaseObjectTx => s.save(page) }
      Action(index)
    }
  }

  def page(title: String) = {
    val pages = pageByTitle(title)
    if (pages.isEmpty) Action(edit(title)) else html.page(title, pages.get(0))
  }

  def pageByTitle(title: String) = {
    ODB withSession { s: ODatabaseObjectTx => s.queryBySql[Page]("select from Page where title = ?", title) }
  }
}
