package com.stackmob.newman

import com.stackmob.common.enumeration._

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
  object HTTPVersionNotSupported extends HttpResponseCode(505, "HTTP Version Not Supported")
}
