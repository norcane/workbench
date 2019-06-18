package com.norcane.workbench

import java.io.File

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.BasicDirectives.{extractLog, extractUnmatchedPath}
import akka.http.scaladsl.server.directives.ContentTypeResolver
import akka.http.scaladsl.server.directives.FileAndResourceDirectives.DirectoryRenderer
import akka.http.scaladsl.server.directives.RouteDirectives.reject

import scala.annotation.tailrec

/**
  * Akka HTTP no longer follow symlinks (see https://github.com/akka/akka-http/issues/365), which is unsuitable for a
  * developer server. Following code is mainly copied from
  * https://github.com/akka/akka-http/blob/master/akka-http/src/main/scala/akka/http/scaladsl/server/directives/FileAndResourceDirectives.scala
  * as there's no proper way how to override private methods.
  */
object CustomDirectives {

  private val followSymlinks = true

  def getFromBrowseableDirectory(directory: String)(implicit renderer: DirectoryRenderer,
                                                    resolver: ContentTypeResolver): Route =
    getFromBrowseableDirectories(directory)

  def getFromBrowseableDirectories(directories: String*)(implicit renderer: DirectoryRenderer,
                                                         resolver: ContentTypeResolver): Route = {
    directories.map(getFromDirectory).reduceLeft(_ ~ _) ~ listDirectoryContents(directories: _*)
  }

  def getFromDirectory(directoryName: String)(implicit resolver: ContentTypeResolver): Route =
    extractUnmatchedPath { unmatchedPath ⇒
      extractLog { log ⇒
        safeDirectoryChildPath(withTrailingSlash(directoryName), unmatchedPath, log) match {
          case "" ⇒ reject
          case fileName ⇒ getFromFile(fileName)
        }
      }
    }

  private def safeDirectoryChildPath(basePath: String,
                                     path: Uri.Path,
                                     log: LoggingAdapter,
                                     separator: Char = File.separatorChar): String =
    safeJoinPaths(basePath, path, log, separator) match {
      case "" ⇒ ""
      case p ⇒ checkIsSafeDescendant(basePath, p, log)
    }

  private def withTrailingSlash(path: String): String = if (path endsWith "/") path else path + '/'

  private def safeJoinPaths(base: String,
                            path: Uri.Path,
                            log: LoggingAdapter,
                            separator: Char = File.separatorChar): String = {
    import java.lang.StringBuilder
    @tailrec def rec(p: Uri.Path, result: StringBuilder = new StringBuilder(base)): String =
      p match {
        case Uri.Path.Empty ⇒ result.toString
        case Uri.Path.Slash(tail) ⇒ rec(tail, result.append(separator))
        case Uri.Path.Segment(head, tail) ⇒
          if (head.indexOf('/') >= 0 || head.indexOf('\\') >= 0 || head == "..") {
            log.warning("File-system path for base [{}] and Uri.Path [{}] contains suspicious path segment [{}], " +
                          "GET access was disallowed",
                        base,
                        path,
                        head)
            ""
          } else rec(tail, result.append(head))
      }
    rec(if (path.startsWithSlash) path.tail else path)
  }

  private def checkIsSafeDescendant(basePath: String, finalPath: String, log: LoggingAdapter): String = {
    val baseFile = new File(basePath)
    val finalFile = new File(finalPath)
    val canonicalFinalPath = finalFile.getCanonicalPath

    if (!followSymlinks && !canonicalFinalPath.startsWith(baseFile.getCanonicalPath)) {
      log.warning(
        s"[$finalFile] points to a location that is not part of [$baseFile]. This might be a directory " +
          "traversal attempt.")
      ""
    } else canonicalFinalPath
  }

}
