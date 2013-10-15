import spark.{Filter, Response, Request, Route}
import spark.Spark._

/**
 *
 * @author sondre
 */
object Service extends App {

  setPort(8080)

  get(new Route("/") {
    def handle(request: Request, response: Response): AnyRef = {
      return "Hello world"
    }
  })

  after(new Filter() {
    def handle(request: Request, response: Response) = response.header("powered-by", "spark")
  })

}
