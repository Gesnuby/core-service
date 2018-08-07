package org.gesnuby.vetclinic.endpoint

import cats.effect.Effect
import org.http4s.HttpService
import org.http4s.dsl.Http4sDsl
import org.http4s.server.staticcontent.WebjarService.Config
import org.http4s.server.staticcontent.{MemoryCache, webjarService}

/**
  * Endpoint for serving static files
  */
class StaticFilesEndpoint[F[_]: Effect] extends Http4sDsl[F] {

  /**
    * Static files from webjars
    */
  private def webjarsEndpoint: HttpService[F] =
    webjarService(Config(cacheStrategy = new MemoryCache[F]))

  def endpoints: HttpService[F] = webjarsEndpoint
}

object StaticFilesEndpoint {
  def endpoints[F[_]: Effect](): HttpService[F] = new StaticFilesEndpoint[F].endpoints
}
