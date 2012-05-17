package com.stackmob.newman.response

import com.stackmob.common.enumeration._
import scalaz._
import Scalaz._

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman
 *
 * User: aaron
 * Date: 4/27/12
 * Time: 4:44 PM
 */


sealed abstract class HttpResponseCode(val code: Int, override val stringVal: String) extends Enumeration

object HttpResponseCode {

  implicit val HttpResponseCodeEqual = new Equal[HttpResponseCode] {
    override def equal(h1: HttpResponseCode, h2: HttpResponseCode) = h1.code === h2.code
  }

  object Accepted extends HttpResponseCode(202, "Accepted")

  object BadGateway extends HttpResponseCode(502, "Bad Gateway")

  object MethodNotAllowed extends HttpResponseCode(405, "Method Not Allowed")

  object BadRequest extends HttpResponseCode(400, "Bad Request")

  object ClientTimeout extends HttpResponseCode(408, "Client Timeout")

  object Conflict extends HttpResponseCode(409, "Conflict")

  object Created extends HttpResponseCode(201, "Created")

  object EntityTooLarge extends HttpResponseCode(413, "Entity Too Large")

  object Forbidden extends HttpResponseCode(403, "Forbidden")

  object GatewayTimeout extends HttpResponseCode(504, "Gateway Timeout")

  object Gone extends HttpResponseCode(410, "Gone")

  object InternalServerError extends HttpResponseCode(500, "Internal Server Error")

  object LengthRequired extends HttpResponseCode(411, "Length Required")

  object MovedPermanently extends HttpResponseCode(301, "Moved Permanently")

  object TemporaryRedirect extends HttpResponseCode(302, "Temporary Redirect")

  object MultipleChoices extends HttpResponseCode(300, "Multiple Choices")

  object NoContent extends HttpResponseCode(204, "No Content")

  object NotAcceptable extends HttpResponseCode(406, "Not Acceptable")

  object NonAuthoritativeInformation extends HttpResponseCode(203, "Non-Authoritative Information")

  object NotFound extends HttpResponseCode(404, "Not Found")

  object NotImplemented extends HttpResponseCode(501, "Not Implemented")

  object NotModified extends HttpResponseCode(304, "Not Modified")

  object Ok extends HttpResponseCode(200, "Ok")

  object PartialContent extends HttpResponseCode(206, "Partial Content")

  object PaymentRequired extends HttpResponseCode(402, "Payment Required")

  object PreconditionFailed extends HttpResponseCode(412, "Precondition Failed")

  object AuthenticationRequired extends HttpResponseCode(407, "Proxy Authentication Required")

  object RequestURITooLarge extends HttpResponseCode(414, "Request-URI Too Large")

  object ResetContent extends HttpResponseCode(205, "Reset Content")

  object SeeOther extends HttpResponseCode(303, "See Other")

  object Unauthorized extends HttpResponseCode(401, "Unauthorized")

  object ServiceUnavailable extends HttpResponseCode(503, "Service Unavailable")

  object UnsupportedMediaType extends HttpResponseCode(415, "Unsupported Media Type")

  object UseProxy extends HttpResponseCode(305, "Use Proxy")

  object HttpVersionNotSupported extends HttpResponseCode(505, "HTTP Version Not Supported")

  def fromInt(i: Int) = i match {
    case Accepted.code => Accepted.some
    case BadGateway.code => BadGateway.some
    case MethodNotAllowed.code => MethodNotAllowed.some
    case BadRequest.code => BadRequest.some
    case ClientTimeout.code => ClientTimeout.some
    case Conflict.code => Conflict.some
    case Created.code => Created.some
    case EntityTooLarge.code => EntityTooLarge.some
    case Forbidden.code => Forbidden.some
    case GatewayTimeout.code => GatewayTimeout.some
    case Gone.code => Gone.some
    case InternalServerError.code => InternalServerError.some
    case LengthRequired.code => LengthRequired.some
    case MovedPermanently.code => MovedPermanently.some
    case TemporaryRedirect.code => TemporaryRedirect.some
    case MultipleChoices.code => MultipleChoices.some
    case NoContent.code => NoContent.some
    case NotAcceptable.code => NotAcceptable.some
    case NonAuthoritativeInformation.code => NonAuthoritativeInformation.some
    case NotFound.code => NotFound.some
    case NotImplemented.code => NotImplemented.some
    case NotModified.code => NotModified.some
    case Ok.code => Ok.some
    case PartialContent.code => PartialContent.some
    case PaymentRequired.code => PaymentRequired.some
    case PreconditionFailed.code => PreconditionFailed.some
    case AuthenticationRequired.code => AuthenticationRequired.some
    case RequestURITooLarge.code => RequestURITooLarge.some
    case ResetContent.code => ResetContent.some
    case SeeOther.code => SeeOther.some
    case Unauthorized.code => Unauthorized.some
    case ServiceUnavailable.code => ServiceUnavailable.some
    case UnsupportedMediaType.code => UnsupportedMediaType.some
    case UseProxy.code => UseProxy.some
    case HttpVersionNotSupported.code => HttpVersionNotSupported.some
    case _ => none
  }
}
