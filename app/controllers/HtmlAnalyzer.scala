package controllers

import java.net.{URI, URISyntaxException}
import javax.inject.{Inject, Singleton}

import model.{Links, Report}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, DocumentType}
import play.api.libs.ws._
import play.api.mvc._

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, TimeoutException}


@Singleton
class HtmlAnalyzer @Inject()(ws: WSClient, cc: ControllerComponents)(
  implicit ec: ExecutionContext) extends AbstractController(cc) {

  import HtmlAnalyzer._

  /**
    * Generates `Report` for a given URL.
    *
    * @param url URL
    * @return `Report`
    */
  def getReport(url: String): Either[String, Report] = {
    try {
      val doc = Jsoup.connect(url).get()

      val (
        reachableLinksCount: Option[Int],
        unreachableLinksCount: Option[Int],
        unreachableLinks: Seq[(String, String)]) = checkLinkedResources(doc) match {

        case Some(xs) => (
          Some(xs.count(_._2 == "OK")),
          Some(xs.count(_._2 != "OK")),
          xs.filter(_._2 != "OK"))

        case _ => (None, None, Seq.empty)
      }

      Right(Report(url = url,
        htmlVersion = getHtmlVersion(doc),
        title = getTitle(doc),
        headingsCount = getHeadingsCount(doc),
        linksCount = getLinksCount(doc),
        login = hasLoginForm(doc),
        reachableLinksCount = reachableLinksCount,
        unreachableLinksCount = unreachableLinksCount,
        unreachableLinks = unreachableLinks
      ))
    } catch {
      case e: Exception => Left(s"%s: %s".format(url, e.getMessage))
    }
  }

  /**
    * For every linked resource from a given document checks if it's reachable. Returns a map of
    * links and results of their retrieval. "OK" means the resource was successfully reached. If
    * it didn't happen, the reason will be mentioned in the result.
    *
    * Tries to access resources in parallel in Futures and then collects the results. Waits for
    * the results for 1 minute, and if it takes longer to reach all the resources returns `None`.
    */
  def checkLinkedResources(doc: Document): Option[Seq[(String, String)]] = {
    val futures: Seq[Future[(String, String)]] = getLinks(doc).map { href =>
      try {
        val url =
          if (Option(new URI(href).getScheme).isDefined) {
            href

          } else if (href.startsWith("//")) {
            val uri1 = new URI(doc.baseUri())
            val uri2 = new URI(href)
            uri1.getScheme + ":" + uri2.getSchemeSpecificPart

          } else if (href.isEmpty || href.startsWith("/")) {
            val uri = new URI(doc.baseUri())
            uri.getScheme + "://" + uri.getHost + href

          } else if (href.startsWith("#")) {
            doc.baseUri() + href

          } else {
            doc.baseUri().substring(0, doc.baseUri().lastIndexOf('/') + 1) + href
          }

        ws.url(url).withFollowRedirects(true).get().map { response =>
          href -> response.statusText
        }
      } catch {
        case e: Exception => Future(href -> s"ERROR: %s".format(e.getMessage))
      }
    }

    try {
      Option(Await.result(Future.sequence(futures), 60 seconds).toMap.toSeq.sortBy(_._1))
    } catch {
      case _: TimeoutException => None
    }
  }
}

object HtmlAnalyzer {

  /** Gets domain name for a given URL. */
  def getDomainName(url: String): Option[String] = {
    try {
      Option(new URI(url).getHost).map { domain =>
        if (domain.startsWith("www.")) domain.substring(4) else domain
      }
    } catch {
      case _: URISyntaxException => None
    }
  }

  /**
    * Retrieves HTML version from a given `Document`.
    *
    * <!doctype html> — HTML5
    * Otherwise gets HTML version from <!DOCTYPE HTML PUBLIC "-//W3C//DTD <HTML version>//EN">
    */
  def getHtmlVersion(doc: Document): Option[String] = {
    doc.childNodes().asScala.collectFirst { case dt: DocumentType => dt } flatMap {
      _.attr("publicId") match {
        case "" => Some("HTML5")
        case s if s.split("//").length > 2 => Some(s.split("//")(2).drop(3).trim)
        case _ => None
      }
    }
  }

  /** Retrieves HTML page title from a given `Document`. */
  def getTitle(doc: Document): Option[String] = {
    doc.title() match {
      case "" => None
      case s => Some(s)
    }
  }

  /**
    * Counts headings in a given `Document`.
    *
    * @return Seq(<heading type> -> <count>), sorted by <heading type>
    */
  def getHeadingsCount(doc: Document): Seq[(String, Int)] = {
    doc.select("h1, h2, h3, h4, h5, h6").asScala.groupBy(_.tagName()).map {
      case (tag, nodes) => tag -> nodes.size
    }.toSeq.sortBy(_._1)
  }

  /**
    * Counts links in a given document.
    *
    * Internal links — to the same domain,
    * external links — to other domains.
    *
    * @return Links(internal, external)
    */
  def getLinksCount(doc: Document): Links = {
    val domain = getDomainName(doc.baseUri())

    getLinks(doc).foldLeft(Links(0, 0)) { case (Links(internal, external), href) =>
      val linkDomain = getDomainName(href)
      if (linkDomain.isEmpty || linkDomain == domain) Links(internal + 1, external)
      else Links(internal, external + 1)
    }
  }

  /**
    * Checks if there's a login form in a given document.
    *
    * Looks for a form that contains
    * - a field of `text` type and name containing {`login`, `loginname`, `user`, `username`}
    * token (case-ignorant);
    * - a field of `password` type.
    **/
  def hasLoginForm(doc: Document): Boolean = {
    doc.select("form").asScala.exists { form =>
      val inputs = form.select("input").asScala

      val loginFound = inputs.exists { input =>
        val loginType = input.attr("type") == "text"
        val loginName = input.attr("name").split('.').map(_.toLowerCase).exists { name =>
          Seq("login", "loginname", "user", "username").contains(name)
        }
        loginType && loginName
      }

      val passwordFound = inputs.exists(_.attr("type") == "password")

      loginFound && passwordFound
    }
  }

  /** Returns all links to other resources found in a given document. */
  def getLinks(doc: Document): Seq[String] = {
    doc.select("a[href]").asScala.map(_.attr("href")) filterNot { href =>
      href == "javascript:;" || href.startsWith("mailto:")
    }
  }

}
