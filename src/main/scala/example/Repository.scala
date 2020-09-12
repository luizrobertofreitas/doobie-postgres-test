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
import Pagination._

object RepositorySingleton {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContexts.synchronous)

  val xa: Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql://localhost:15432/test_test",
    "user",
    "pass",
    Blocker.liftExecutionContext(ExecutionContexts.synchronous)
  )

  def apply(): Repository = new Repository(xa)
}

class Repository(xa: Transactor[IO]) {
  import RepositorySQL._
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
      master <- insertMaster(master.copy(id = Some(masterId), description = s"${master.description}-$masterId"))
      detail <- insertDetail(detail.copy(masterId = Some(masterId), description = s"${detail.description}-$masterId"))
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

  def findAllMasterDetail(page: Int = 0, pageSize: Int = 10): Page[(Master, Detail)] =
    paginated[(Master, Detail)](page, pageSize)(selectAllMasterJoinDetail, countAllMasterJoinDetail)


  def paginated[A: Read](page: Int, pageSize: Int)(q: Query0[A], count: Query0[Long]): Page[A] = {
    val pagedResult = paginate(pageSize, page * pageSize)(q)
      .to[Seq]
      .transact(xa)
      .unsafeRunSync
    val countResult = count.unique.transact(xa).unsafeRunSync
    Page[A](page, pageSize, countResult, pagedResult)
  }
}

private object RepositorySQL {
  def selectAllMasterJoinDetail: Query0[(Master, Detail)] =
    fr"select m.*, d.* from master m left outer join detail d on m.id = d.master_id".query

  def countAllMasterJoinDetail: Query0[Long] =
    fr"select count(1) from master m left outer join detail d on m.id = d.master_id".query


}
