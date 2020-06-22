package example

import doobie._
import doobie.implicits._
import doobie.util.ExecutionContexts
import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import doobie.util.transactor.Transactor.Aux
import fs2.Stream
import io.circe._
import io.circe.jawn._
import io.circe.syntax._
import org.postgresql.util.PGobject

object RepositorySingleton {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContexts.synchronous)

  val xa: Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql://localhost:5432/doobie_test",
    "postgres",
    "postgres",
    Blocker.liftExecutionContext(ExecutionContexts.synchronous)
  )

  def apply(): Repository = new Repository(xa)
}

class Repository(xa: Transactor[IO]) {

  import io.circe.generic.semiauto.deriveDecoder
  import io.circe.generic.semiauto.deriveEncoder

  implicit val comDecoder: Decoder[Person] = deriveDecoder[Person]
  implicit val comEncoder: Encoder[Person] = deriveEncoder[Person]

  implicit val jsonMeta: Meta[Person] =
    Meta.Advanced.other[PGobject]("json").timap[Person](
      a => decode[Person](a.getValue).leftMap[Person](e => throw e).merge)(
      a => {
        val o = new PGobject
        o.setType("json")
        o.setValue(a.asJson.noSpaces)
        o
      }
    )

  def test(): Unit = {
    sql"select id, info from omg"
      .query[Omg]
      .stream
      .take(5)
      .compile.toList
      .transact(xa)
      .unsafeRunSync
      .foreach(println)
  }

}
