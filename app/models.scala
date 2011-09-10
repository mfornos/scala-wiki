package models

import javax.persistence.{ Version, Id }
import play.data.validation._

case class User {
  @Id var id: String = _
  @Required var name: String = _
  var addresses: java.util.List[Address] = new java.util.ArrayList()
  @Version var version: String = _

  override def toString = "User: " + this.id + ", name: " + this.name + ", addresses: " + this.addresses
}

class Address {
  var city: String = _
  @Required var street: String = _

  override def toString = "Address: " + this.city + ", " + this.street
}

class Page {
  @Id var id: String = _
  @Required @MinSize(2) @MaxSize(255) var title: String = _
  @Required @MinSize(10) @MaxSize(1000) var description: String = _
  @MaxSize(50000) var content: String = _
  var user: User = _
  @Version var version: String = _

  override def toString = "Page: " + this.id + ", title: " + this.title + ", belongs: " + this.user
}
