import com.theoryinpractise.halbuilder.api.RepresentationFactory
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import spark.{Filter, Response, Request, Route}
import spark.Spark._

/**
 *
 * @author sondre
 */
object Service extends App {

  val userBuffer: mutable.Buffer[User] = ArrayBuffer.apply(new User(1L, "User"))
  val representationFactory = new StandardRepresentationFactory

  setPort(8080)

  get(IndexRoute)

  get(UserListRoute)

  after(new Filter() {
    def handle(request: Request, response: Response) = response.header("powered-by", "spark")
  })

}

case class User(id: Long, name: String)

class IndexRoute(path: String) extends Route(path) {
  def handle(request: Request, response: Response): AnyRef = {
    import Service._
    representationFactory.newRepresentation(path)
      .withLink("index", Paths.usersPath)
      .withProperty("service", "User service")
      .withProperty("version", "1.0.0-SNAPSHOT")
      .toString(RepresentationFactory.HAL_JSON)
  }
}

object IndexRoute extends IndexRoute("/") {
}


class UserListRoute(path: String) extends Route(path) {

  def handle(request: Request, response: Response): AnyRef = {
    import Service._
    val resource = representationFactory.newRepresentation(path)

    userBuffer.foreach {
      user => {
        val userId = user.id
        resource.withRepresentation("users", representationFactory.newRepresentation(s"$path/$userId")
          .withNamespace(Namespaces.user._1, Namespaces.user._2)
          .withLink(Namespaces.user._1 + ":item",Paths.userItemPath )
          .withProperty("id", user.id)
          .withProperty("name", user.name))

      }
    }
    return resource.toString(RepresentationFactory.HAL_JSON)
  }
}

object UserListRoute extends UserListRoute(Paths.usersPath) {
  val representation = "users"
}

object UserItemRoute extends Route(Paths.usersPath) {
  def handle(request: Request, response: Response): AnyRef = {

  }
}

object Paths {
  val usersPath = "/users"
  val userItemPath = "/users/{id}"
}

object Namespaces {
  val user = ("user", "http://wiki.nextgentel.net/display/SYS/hal-elements#{rel}")
}

