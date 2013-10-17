import com.theoryinpractise.halbuilder.api.{ReadableRepresentation, RepresentationFactory}
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory
import java.io.StringReader
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

  def findUserById(id: Long): Option[User] = userBuffer find {
    user => id == user.id
  }

  setPort(lookupPort)

  staticFileLocation("browser")

  get(IndexRoute)

  get(UserListRoute)

  get(UserItemRoute)

  put(UserAddRoute)

  after(new Filter() {
    def handle(request: Request, response: Response) = response.header("powered-by", "spark")
  })

}

case class User(id: Long, name: String) {
  def getId = id

  def getName = name
}

import Service._

class IndexRoute(path: String) extends Route(path) {
  def handle(request: Request, response: Response): AnyRef = {
    import Service._
    representationFactory.newRepresentation(path)
      .withLink("index", Paths.usersPath, "allUsers", "List of all users", null, null)
      .withProperty("service", "User service")
      .withProperty("version", "1.0.0-SNAPSHOT")
      .toString(RepresentationFactory.HAL_JSON)
  }
}

object IndexRoute extends IndexRoute("/") {
}


class UserListRoute(path: String) extends Route(path) {

  def handle(request: Request, response: Response): AnyRef = {
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

object UserAddRoute extends Route(Paths.usersPath) {

  def extractUserFromContent(body: String): Option[User]  = {
    val representation = representationFactory.readRepresentation(new StringReader(body))
    val user = (representation: ReadableRepresentation) => {
      import scala.collection.JavaConverters._
      val resourceProperties = representation.getProperties.asScala
      resourceProperties.get("id") map {
        id => id.toString.toLong
      } map {
        userId => (userId, (resourceProperties.get("name") map {_.toString} map {_.trim}).getOrElse(""))
      } map {
        t => new User(t._1, t._2)
      }
    }
    user(representation)
  }

  def handle(request: Request, response: Response): AnyRef = {
    extractUserFromContent(request.body()) match {
      case Some(user) => findUserById(user.id) match {
        case Some(existingUser) => {
          response.status(HttpServletResponse.SC_BAD_REQUEST)
          val id = user.id
          s"Can not create user with id $id because an User with this id exists. Note: to update the existing user, issue a POST"
        }
        case None => {
          userBuffer += user
          response.status(HttpServletResponse.SC_CREATED)
          s"User created"
        }
      }
      case None => {
        response.status(HttpServletResponse.SC_BAD_REQUEST)
        "No valid user data found in request"
      }
    }

  }
}

object UserItemRoute extends Route(Paths.userItemPath.replace('{', ':').replace("}", "")) {

  def extractIdFromRequest(request: Request): Option[Long] = {
    request.params("id") match {
      case id: String => Some(id.toLong)
      case _ => None
    }
  }



  def handle(request: Request, response: Response): AnyRef = {

    val res = extractIdFromRequest(request) map {
      userId => findUserById(userId) match {
        case None => {
          response.status(HttpServletResponse.SC_NOT_FOUND)
          s"No user found with id $userId"
        }
        case Some(user) => representationFactory.newRepresentation(Paths.itemPathForId(userId))
          .withBean(user)
          .withLink("index", Paths.usersPath, "all-users", "All users", null, null)
          .toString(RepresentationFactory.HAL_JSON)
      }

    }
    res.getOrElse("")
  }
}

object Paths {
  val usersPath = "/users"
  val userItemPath = "/users/{id}"

  def itemPathForId(id: Long) = s"$usersPath/$id"
}

object Namespaces {
  val user = ("user", "https://github.com/zapodot/hal-example/wiki#{rel}")
}

