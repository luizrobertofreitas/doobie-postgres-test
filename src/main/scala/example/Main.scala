package example

import example.model.{Detail, Master}

object Main extends App {

  val Repository: Repository = RepositorySingleton()

  println(s"Simple computation: ${Repository.runSimpleComputation}")

  val (master, detail) = Repository.insertMasterAndDetail(
    Master(description = "Master"),
    Detail(description = "Detail")
  )

  println(master)
  println(detail)
}
