package controllers

import model.Links
import org.jsoup.nodes.{Document, DocumentType, Element}
import org.scalatest.{FunSpec, Matchers}


class HtmlAnalyzerSpec extends FunSpec with Matchers {

  describe("getDomainName") {
    it("should return `None` if URL is malformed") {
      HtmlAnalyzer.getDomainName("foo") should be(None)
    }

    it("should return `None` if URL doesn't have `http` prefix") {
      HtmlAnalyzer.getDomainName("www.foo.com") should be(None)
    }

    it("should return domain for `http` URL") {
      HtmlAnalyzer.getDomainName("http://foo.com") should be(Some("foo.com"))
    }

    it("should return domain for `https` URL") {
      HtmlAnalyzer.getDomainName("https://foo.com") should be(Some("foo.com"))
    }

    it("shouldn't include `www` to domain name") {
      HtmlAnalyzer.getDomainName("http://www.foo.com") should be(Some("foo.com"))
    }
  }

  describe("getHtmlVersion") {
    it("should return `HTML5` for `<!doctype html>`") {
      val doc = new Document("http://foo.com")
      val dt = new DocumentType("html", "", "", "")
      doc.appendChild(dt)

      HtmlAnalyzer.getHtmlVersion(doc) should be(Some("HTML5"))
    }

    it("should return `HTML 4.01` for `-//W3C//DTD HTML 4.01//EN` in `doctype`") {
      val doc = new Document("http://foo.com")
      val dt = new DocumentType("html", "-//W3C//DTD HTML 4.01//EN", "", "")
      doc.appendChild(dt)

      HtmlAnalyzer.getHtmlVersion(doc) should be(Some("HTML 4.01"))
    }

    it("should return `None` if there's no `doctype`") {
      HtmlAnalyzer.getHtmlVersion(new Document("http://foo.com")) should be(None)
    }
  }

  describe("getTitle") {
    it("should return `None` if there's no title") {
      HtmlAnalyzer.getTitle(new Document("http://foo.com")) should be(None)
    }

    it("should return title") {
      val doc = new Document("http://foo.com")
      val head = new Element("head")
      val title = new Element("title")
      title.text("Test Title")
      head.appendChild(title)
      doc.appendChild(head)

      HtmlAnalyzer.getTitle(doc) should be(Some("Test Title"))
    }
  }

  describe("getHeadings") {
    it("should calculate headings of all types") {
      val doc = new Document("http://foo.com")
      val body = new Element("body")
      val h1 = new Element("h1")
      val h2 = new Element("h2")
      val h3 = new Element("h3")
      val h4 = new Element("h4")
      val h5 = new Element("h5")
      val h6 = new Element("h6")

      body.appendChild(h1)
      body.appendChild(h2)
      body.appendChild(h3)
      body.appendChild(h4)
      body.appendChild(h5)
      body.appendChild(h6)
      doc.appendChild(body)

      val result = Seq("h1" -> 1, "h2" -> 1, "h3" -> 1, "h4" -> 1, "h5" -> 1, "h6" -> 1)

      HtmlAnalyzer.getHeadingsCount(doc) should be(result)
    }

    it("should calculate headings inside other tags") {
      val doc = new Document("http://foo.com")
      val body = new Element("body")
      val div1 = new Element("div")
      val div2 = new Element("div")
      val h1 = new Element("h1")
      val h2 = new Element("h1")

      div2.appendChild(h1)
      div1.appendChild(div2)
      div1.appendChild(h2)
      body.appendChild(div1)
      doc.appendChild(body)

      val result = Seq("h1" -> 2)

      HtmlAnalyzer.getHeadingsCount(doc) should be(result)
    }
  }

  describe("getLinks") {
    it("should calculate links") {
      val doc = new Document("http://foo.com")
      val body = new Element("body")

      val a1 = new Element("a")
      a1.attr("href", "http://foo.com/bar")

      val a2 = new Element("a")
      a2.attr("href", "http://foo.com/bar/buzz")

      val a3 = new Element("a")
      a3.attr("href", "http://google.com")

      val a4 = new Element("a")
      a4.attr("href", "/local/link")

      body.appendChild(a1)
      body.appendChild(a2)
      body.appendChild(a3)
      body.appendChild(a4)
      doc.appendChild(body)

      HtmlAnalyzer.getLinksCount(doc) should be(Links(3, 1))
    }

    it("should calculate links inside other tags") {
      val doc = new Document("http://foo.com")
      val body = new Element("body")
      val div1 = new Element("div")
      val div2 = new Element("div")

      val a1 = new Element("a")
      a1.attr("href", "http://foo.com/bar")

      val a2 = new Element("a")
      a2.attr("href", "http://google.com")

      div2.appendChild(a2)
      div1.appendChild(div2)
      div1.appendChild(a1)
      body.appendChild(div1)
      doc.appendChild(body)

      HtmlAnalyzer.getLinksCount(doc) should be(Links(1, 1))
    }
  }
}

