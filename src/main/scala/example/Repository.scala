package example

import doobie._
import doobie.implicits._
import doobie.util.ExecutionContexts
import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import doobie.util.transactor.Transactor.Aux
import example.model.{Detail, Master, Omg, Person}
import fs2.Stream
import io.circe._
import io.circe.jawn._
import io.circe.syntax._
import org.postgresql.util.PGobject

object RepositorySingleton {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContexts.synchronous)

  val xa: Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql://localhost:15432/client_billing",
    "gympass",
    "1234qwer",
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

  def simpleComputation: ConnectionIO[Int] = 42.pure[ConnectionIO]

  def runSimpleComputation: Int = simpleComputation.transact(xa).unsafeRunSync

  def insertMasterAndDetail(master: Master, detail: Detail): (Master, Detail) = {
    val transaction = for {
      masterId <- masterId
      master <- insertMaster(master.copy(id = Some(masterId)))
      detail <- insertDetail(detail.copy(masterId = Some(masterId)))
    } yield (master, detail)

    transaction.transact(xa).unsafeRunSync()
  }

  def masterId: ConnectionIO[Long] =
    sql"""select nextval('master_id_seq')""".query[Long].unique

  def insertMaster(master: Master): ConnectionIO[Master] =
    sql"""insert into master (id, description) values (${master.id}, ${master.description})"""
      .stripMargin
      .update
      .withUniqueGeneratedKeys[Master]("id", "description")

  def insertDetail(detail: Detail): ConnectionIO[Detail] =
    sql"""insert into detail (master_id, description) values (${detail.masterId}, ${detail.description})"""
      .stripMargin
      .update
      .withUniqueGeneratedKeys[Detail]("id", "master_id", "description")

  def findAllOmg(): Unit = {
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
