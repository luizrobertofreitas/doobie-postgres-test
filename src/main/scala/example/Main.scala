package example

object Main extends App {

  val Repository: Repository = RepositorySingleton()

  Repository.test()

}
