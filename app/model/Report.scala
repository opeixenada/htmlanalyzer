package model


case class Report(url: String,
                  htmlVersion: Option[String],
                  title: Option[String],
                  headingsCount: Seq[(String, Int)],
                  linksCount: Links,
                  login: Boolean,
                  reachableLinksCount: Option[Int],
                  unreachableLinksCount: Option[Int],
                  unreachableLinks: Seq[(String, String)])

case class Links(internal: Int, external: Int)
