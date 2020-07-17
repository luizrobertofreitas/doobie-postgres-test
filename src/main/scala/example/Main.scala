package example

import example.model.{Detail, Master}

object Main extends App {

  val Repository: Repository = RepositorySingleton()

  println(s"Simple computation: ${Repository.runSimpleComputation}")

  (1 to 100) map { _ =>
    Repository.insertMasterAndDetail(
      Master(description = "Master"),
      Detail(description = "Detail")
    )
  }
  println("*"*88)
  val joinQueryResult = Repository.findAllMasterDetail()
  joinQueryResult.results.foreach(println)
  println("*"*88)
}
