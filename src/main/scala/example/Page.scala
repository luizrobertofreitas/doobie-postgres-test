package example

case class Page[T](page: Int, pageSize: Int, totalElements: Long, results: Seq[T])
