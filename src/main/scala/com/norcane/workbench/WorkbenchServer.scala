package com.norcane.workbench

import java.util.concurrent.atomic.AtomicReference

import akka.actor.{ActorSystem, Props, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.coding.{Encoder, Gzip, NoCoding}
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.settings.ServerSettings
import akka.stream.ActorMaterializer
import com.norcane.workbench.shared.ReadWrite
import com.typesafe.config.ConfigFactory
import sbt.IO
import upickle.default.{Reader, Writer}

import scala.concurrent.{Future, _}
import scala.util.{Failure, Success}

class WorkbenchServer(url: String,
                      port: Int,
                      defaultRootObject: Option[String] = None,
                      rootDirectory: Option[String] = None,
                      useCompression: Boolean = false) {

  import WorkbenchActor.Message

  private val classLoader = getClass.getClassLoader
  private implicit val system: ActorSystem =
    ActorSystem("Workbench-System", config = ConfigFactory.load(classLoader), classLoader = classLoader)
  private val workbenchActor = system.actorOf(Props[WorkbenchActor], "workbench-actor")

  private val corsHeaders: List[ModeledHeader] = List(
    `Access-Control-Allow-Methods`(OPTIONS, GET, POST),
    `Access-Control-Allow-Origin`(HttpOriginRange.*),
    `Access-Control-Allow-Headers`(
      "Origin, X-Requested-With, Content-Type, Accept, Accept-Encoding, Accept-Language, Host, Referer, User-Agent"),
    `Access-Control-Max-Age`(1728000)
  )

  /**
    * The connection from workbench server to the client
    */
  object Wire extends autowire.Client[ujson.Value, Reader, Writer] with ReadWrite {
    def doCall(req: Request): Future[ujson.Value] = {
      workbenchActor ! Message.QueueMessage(ujson.Arr(upickle.default.writeJs(req.path), ujson.Obj.from(req.args)))
      Future.successful(ujson.Null)
    }
  }

  /**
    * Simple spray server:
    *
    * - /workbench.js is hardcoded to be the workbench javascript client
    * - Any other GET request just pulls from the local filesystem
    * - POSTs to /notifications get routed to the Workbench Actor
    */
  private implicit val materializer: ActorMaterializer = ActorMaterializer()
  // needed for the future map/flatmap in the end
  private implicit val executionContext: ExecutionContext = system.dispatcher

  private val serverBinding = new AtomicReference[Http.ServerBinding]()
  private val encoder: Encoder = if (useCompression) Gzip else NoCoding

  var serverStarted = false

  def startServer(): Unit = {
    if (serverStarted) return
    serverStarted = true
    val bindingFuture =
      Http().bindAndHandle(handler = routes, interface = url, port = port, settings = ServerSettings(system))

    bindingFuture.onComplete {
      case Success(binding) ⇒
        //setting the server binding for possible future uses in the client
        serverBinding.set(binding)
        system.log.info(s"Server online at http://${binding.localAddress.getHostName}:${binding.localAddress.getPort}/")

      case Failure(cause) ⇒
        system.log.error(cause, "Cannot bind server.")
    }
  }

  lazy val routes: Route =
    encodeResponseWith(encoder) {
      get {
        path("workbench.js") {
          val body = IO.readStream(getClass.getClassLoader.getResourceAsStream("client-opt.js"))
          val response = s"""
                            |(function(){
                            |  $body
                            |
                            |  WorkbenchClient.main(${upickle.default.write(url)}, ${upickle.default.write(port)})
                            |}).call(this)
             """.stripMargin

          val contentType = ContentType(MediaTypes.`application/javascript`, HttpCharsets.`UTF-8`)
          complete(HttpResponse(entity = HttpEntity(contentType, response)))
        }
      } ~
        pathSingleSlash {
          getFromFile(defaultRootObject.getOrElse(""))
        } ~
        CustomDirectives.getFromBrowseableDirectories(rootDirectory.getOrElse(".")) ~
        post {
          path("notifications") {
            val promise = Promise[ujson.Arr]
            workbenchActor ! Message.AddWaitingClient(promise)
            onSuccess(promise.future) { output =>
              complete(
                HttpResponse(
                  entity = HttpEntity(ContentType(MediaTypes.`application/json`), upickle.default.write(output)))
                  .withHeaders(corsHeaders: _*))
            }
          }
        }
    }

  def kill(): Future[Terminated] = {
    system.terminate()
  }
}
