package com.stackmob.newman

import org.apache.http.Header
import org.apache.http.message.BasicHeader
import com.stackmob.core.proxy.{ProxyResponse, ProxyRouter, ProxyRequest}

import com.stackmob.core.translatable.api.HttpVerb

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman
 *
 * User: Kelsey
 * Date: 4/24/12
 * Time: 4:58 PM
 */


object AugmentedProxyRequest{
  implicit def stringToURL(url: String) : CompleteRequest = GET(url)

  def POST(path: String): URLRequestNeedsBody = URLRequestNeedsBody(url = path, method = HttpVerb.POST)
  def PUT(path: String): URLRequestNeedsBody = URLRequestNeedsBody(url = path, method = HttpVerb.PUT)
  def GET(path: String): CompleteRequest = CompleteRequest(url = path, method = HttpVerb.GET, reqBody = None)
  def DELETE(path: String): CompleteRequest = CompleteRequest(url = path, method = HttpVerb.DELETE, reqBody = None)

  case class CompleteRequest protected[AugmentedProxyRequest](url: String,
                                params: Option[Map[String, List[String]]] = None,
                                reqHeaders: Option[List[Header]] = None,
                                method: HttpVerb,
                                reqBody: Option[String]) extends BaseURLRequest[CompleteRequest]{

    def makeCopy(url: String = url,
                 params: Option[Map[String, List[String]]] = params,
                 reqBody: Option[String] = reqBody,
                 reqHeaders: Option[List[Header]] = reqHeaders,
                 method: HttpVerb = method) = this.copy(url, params, reqHeaders, method, reqBody)

    //this is the method that needs to change to call your code
    def execute(implicit proxyRouter: ProxyRouter): ProxyResponse =  {

      implicit def AugmentedReqToReq(aug: CompleteRequest) : ProxyRequest = {
        ProxyRequest(aug.url, aug.params getOrElse null, aug.reqBody getOrElse null, aug.reqHeaders getOrElse null)
      }

      method match {
        case HttpVerb.GET => proxyRouter.doHttpGet(req)
        case HttpVerb.POST => proxyRouter.doHttpPost(req)
        case HttpVerb.PUT => proxyRouter.doHttpPut(req)
        case HttpVerb.DELETE => proxyRouter.doHttpDelete(req)
      }
    }
  }

  case class URLRequestNeedsBody protected[AugmentedProxyRequest](url: String,
                                 params: Option[Map[String, List[String]]] = None,
                                 reqHeaders: Option[List[Header]] = None,
                                 method: HttpVerb) extends BaseURLRequest[URLRequestNeedsBody]{
    def makeCopy(url: String = url,
                 params: Option[Map[String, List[String]]] = params,
                 reqBody: Option[String],
                 reqHeaders: Option[List[Header]] = reqHeaders,
                 method: HttpVerb = method) = this.copy(url, params, reqHeaders, method)

    def bodyCreator[T <: AnyRef]() = (t:T) => JSONUtil.serialize[T](t)
    def body(obj: AnyRef): CompleteRequest = CompleteRequest(url, params, reqHeaders, method, reqBody = Option(bodyCreator[obj.type].apply(obj)))
  }

  sealed abstract class BaseURLRequest[ChildReq <: BaseURLRequest[_]](){

    val url: String
    val params: Option[Map[String, List[String]]]
    val reqHeaders: Option[List[Header]]
    val method: HttpVerb

    def makeCopy(url: String = url,
                 params: Option[Map[String, List[String]]] = params,
                 reqBody: Option[String] = None,
                 reqHeaders: Option[List[Header]] = reqHeaders,
                 method: HttpVerb = method) : ChildReq

    def headers(newHeaders: => Map[String, String]): ChildReq = {
      val headerList = reqHeaders openOr Nil
      this.makeCopy(reqHeaders = Option(newHeaders.toList.map({kv: (String, String) => new BasicHeader(kv._1, kv._2)}) ++ headerList))
    }

    def params(newParams: => Map[String, String]): ChildReq = {
      this.makeCopy(params = Option(newParams.map({pair: (String, String) => (pair._1, List(pair._2))})))
    }

    def params(newParams: Map[String, List[String]]): ChildReq = {
      this.makeCopy(params = Option(newParams))
    }
  }
}