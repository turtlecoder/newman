package com.stackmob.newman.dsl

//import org.apache.http.Header
//import org.apache.http.message.BasicHeader
//import com.stackmob.common.enumeration._

/**
* Created by IntelliJ IDEA.
*
* com.stackmob.newman
*
* User: Kelsey
* Date: 4/24/12
* Time: 4:58 PM
*/

//Aaron, 4/27/2012 commenting candidate implementation that doesn't compile

//sealed abstract class HttpVerb(override val stringVal: String) extends Enumeration
//object HttpVerb {
//  object POST extends HttpVerb("POST")
//  object PUT extends HttpVerb("PUT")
//  object GET extends HttpVerb("GET")
//  object DELETE extends HttpVerb("DELETE")
//}
//
//object AugmentedProxyRequest{
//  implicit def stringToURL(url: String) : CompleteRequest = GET(url)
//
//  def POST(path: String): URLRequestNeedsBody = URLRequestNeedsBody(url = path, method = HttpVerb.POST)
//  def PUT(path: String): URLRequestNeedsBody = URLRequestNeedsBody(url = path, method = HttpVerb.PUT)
//  def GET(path: String): CompleteRequest = CompleteRequest(url = path, method = HttpVerb.GET, reqBody = None)
//  def DELETE(path: String): CompleteRequest = CompleteRequest(url = path, method = HttpVerb.DELETE, reqBody = None)
//
//  case class CompleteRequest protected[AugmentedProxyRequest](url: String,
//                                params: Option[Map[String, List[String]]] = None,
//                                reqHeaders: Option[List[Header]] = None,
//                                method: HttpVerb,
//                                reqBody: Option[String]) extends BaseURLRequest[CompleteRequest]{
//
//    def makeCopy(url: String = url,
//                 params: Option[Map[String, List[String]]] = params,
//                 reqBody: Option[String] = reqBody,
//                 reqHeaders: Option[List[Header]] = reqHeaders,
//                 method: HttpVerb = method) = this.copy(url, params, reqHeaders, method, reqBody)
//
//    //this is the method that needs to change to call your code
//    def execute: HttpResponse = {
//
//
//      method match {
//        case HttpVerb.GET => proxyRouter.doHttpGet(req)
//        case HttpVerb.POST => proxyRouter.doHttpPost(req)
//        case HttpVerb.PUT => proxyRouter.doHttpPut(req)
//        case HttpVerb.DELETE => proxyRouter.doHttpDelete(req)
//      }
//    }
//  }
//
//  case class URLRequestNeedsBody protected[AugmentedProxyRequest](url: String,
//                                 params: Option[Map[String, List[String]]] = None,
//                                 reqHeaders: Option[List[Header]] = None,
//                                 method: HttpVerb) extends BaseURLRequest[URLRequestNeedsBody]{
//    def makeCopy(url: String = url,
//                 params: Option[Map[String, List[String]]] = params,
//                 reqBody: Option[String],
//                 reqHeaders: Option[List[Header]] = reqHeaders,
//                 method: HttpVerb = method) = this.copy(url, params, reqHeaders, method)
//
//    def bodyCreator[T <: AnyRef]() = (t:T) => JSONUtil.serialize[T](t)
//    def body(obj: AnyRef): CompleteRequest = CompleteRequest(url, params, reqHeaders, method, reqBody = Option(bodyCreator[obj.type].apply(obj)))
//  }
//
//  sealed abstract class BaseURLRequest[ChildReq <: BaseURLRequest[_]](){
//
//    val url: String
//    val params: Option[Map[String, List[String]]]
//    val reqHeaders: Option[List[Header]]
//    val method: HttpVerb
//
//    def makeCopy(url: String = url,
//                 params: Option[Map[String, List[String]]] = params,
//                 reqBody: Option[String] = None,
//                 reqHeaders: Option[List[Header]] = reqHeaders,
//                 method: HttpVerb = method) : ChildReq
//
//    def headers(newHeaders: => Map[String, String]): ChildReq = {
//      val headerList = reqHeaders openOr Nil
//      this.makeCopy(reqHeaders = Option(newHeaders.toList.map({kv: (String, String) => new BasicHeader(kv._1, kv._2)}) ++ headerList))
//    }
//
//    def params(newParams: => Map[String, String]): ChildReq = {
//      this.makeCopy(params = Option(newParams.map({pair: (String, String) => (pair._1, List(pair._2))})))
//    }
//
//    def params(newParams: Map[String, List[String]]): ChildReq = {
//      this.makeCopy(params = Option(newParams))
//    }
//  }
//}