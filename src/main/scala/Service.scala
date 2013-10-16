import com.theoryinpractise.halbuilder.api.RepresentationFactory
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory
import javax.servlet.http.HttpServletResponse
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
  def lookupPort = {
    sys.env.get("PORT").getOrElse("8080").toInt
  }

  setPort(lookupPort)

  staticFileLocation("browser")

  get(IndexRoute)

  get(UserListRoute)

  get(UserItemRoute)

  after(new Filter() {
    def handle(request: Request, response: Response) = response.header("powered-by", "spark")
  })

}

case class User(id: Long, name: String) {
  def getId = id

  def getName = name
}

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
    val resource = representationFactory.newRepresentation(path).withNamespace(Namespaces.user._1, Namespaces.user._2)

    userBuffer.foreach {
      user => {
        val userId = user.id
        resource.withRepresentation("users", representationFactory.newRepresentation(s"$path/$userId")
          .withLink(Namespaces.user._1 + ":item", Paths.userItemPath)
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

object UserItemRoute extends Route(Paths.userItemPath) {

  def extractIdFromRequest(request: Request): Option[Long] = {
    request.params("id") match {
      case id: String => Some(id.toLong)
      case _ => None
    }
  }

  def findUser(id: Long): Option[User] = Service.userBuffer find {
    user => id == user.id
  }

  def handle(request: Request, response: Response): AnyRef = {

    val res = extractIdFromRequest(request) map {
      userId => findUser(userId) match {
        case None => {
          response.status(HttpServletResponse.SC_NOT_FOUND)
          s"No user found with id $userId"
        }
        case Some(user) => Service.representationFactory.newRepresentation(Paths.userItemPath.replace(":id", userId.toString))
          .withBean(user)
          .withLink("index", Paths.usersPath)
          .toString(RepresentationFactory.HAL_JSON)
      }

    }
    res.getOrElse("")
  }
}

object Paths {
  val usersPath = "/users"
  val userItemPath = "/users/:id"
}

object Namespaces {
  val user = ("user", "https://github.com/zapodot/hal-example/wiki#{rel}")
}

