package example

object MainQuery extends App {
  val Repository: Repository = RepositorySingleton()

  println("*"*88)
  (0 to 3).map { i =>
    val joinQueryResult = Repository.findAllMasterDetail(i, 5)
    println(s"Page: ${joinQueryResult.page}")
    println(s"Page Size: ${joinQueryResult.pageSize}")
    println(s"Offset: ${joinQueryResult.pageSize * joinQueryResult.page}")
    println(s"Total Elements: ${joinQueryResult.totalElements}")
    joinQueryResult.results.foreach(println)
  }
  println("*"*88)
}
