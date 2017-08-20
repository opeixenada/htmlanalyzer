package controllers

import javax.inject.{Inject, Singleton}

import model.UrlData
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n._
import play.api.mvc._


@Singleton
class MainController @Inject()(hac: HtmlAnalyzer, cc: ControllerComponents)
  extends AbstractController(cc) with I18nSupport {

  private val urlForm = Form(
    mapping(
      "url" -> text
    )(UrlData.apply)(UrlData.unapply)
  )

  /** GET empty index page. */
  def index = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.main(urlForm, None, None))
  }

  /**
    * POST `UrlData`
    * Retrieves and shows report for a given URL.
    **/
  def analyzeHtml = Action { implicit request: Request[AnyContent] =>
    val errorFunction = { formWithErrors: Form[UrlData] =>
      BadRequest(views.html.main(formWithErrors, Some("Form validation error"), None))
    }

    val successFunction = { data: UrlData =>
      val reportResult = hac.getReport(data.url)
      Ok(views.html.main(urlForm, reportResult.left.toOption, reportResult.toOption))
    }

    val formValidationResult = urlForm.bindFromRequest
    formValidationResult.fold(errorFunction, successFunction)
  }
}