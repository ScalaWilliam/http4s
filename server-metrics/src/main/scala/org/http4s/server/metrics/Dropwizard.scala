package org.http4s.server.metrics

import cats.effect.Sync
import com.codahale.metrics.MetricRegistry
import java.util.concurrent.TimeUnit
import org.http4s.{Method, Status}

object Dropwizard {

  /**
    * Creates a [[MetricsOps]] that supports Dropwizard metrics
    *
    * @param registry a dropwizard metric registry
    * @param prefix a prefix that will be added to all metrics
    */
  def apply[F[_]](registry: MetricRegistry, prefix: String)(implicit F: Sync[F]): MetricsOps[F] =
    new MetricsOps[F] {

      override def increaseActiveRequests(classifier: Option[String]): F[Unit] = F.delay {
        registry.counter(s"${namespace(prefix, classifier)}.active-requests").inc()
      }

      override def decreaseActiveRequests(classifier: Option[String]): F[Unit] = F.delay {
        registry.counter(s"${namespace(prefix, classifier)}.active-requests").dec()
      }

      override def increaseRequests(elapsed: Long, classifier: Option[String]): F[Unit] = F.delay {
        registry
          .timer(s"${namespace(prefix, classifier)}.requests")
          .update(elapsed, TimeUnit.NANOSECONDS)
      }

      override def recordHeadersTime(elapsed: Long, classifier: Option[String]): F[Unit] = F.delay {
        registry
          .timer(s"${namespace(prefix, classifier)}.requests.headers")
          .update(elapsed, TimeUnit.NANOSECONDS)
      }

      override def recordTotalTime(method: Method, status: Status, elapsed: Long, classifier: Option[String]): F[Unit] =
        F.delay {
          registry
            .timer(s"${namespace(prefix, classifier)}.requests.total")
            .update(elapsed, TimeUnit.NANOSECONDS)
          registerStatusCode(status, elapsed, classifier)
        }

      override def recordTotalTime(method: Method, elapsed: Long,  classifier: Option[String]): F[Unit] =
        F.delay {
          registry
            .timer(s"${namespace(prefix, classifier)}.${requestTimer(method)}")
            .update(elapsed, TimeUnit.NANOSECONDS)
        }

      override def recordTotalTime(status: Status, elapsed: Long, classifier: Option[String]): F[Unit] =
        F.delay {
          registerStatusCode(status, elapsed, classifier)
        }

      override def increaseErrors(elapsed: Long, classifier: Option[String]): F[Unit] = F.delay {
        registry
          .timer(s"${namespace(prefix, classifier)}.errors")
          .update(elapsed, TimeUnit.NANOSECONDS)
      }

      override def increaseTimeouts(classifier: Option[String]): F[Unit] = F.delay {
      }

      override def increaseAbnormalTerminations(elapsed: Long, classifier: Option[String]): F[Unit] = F.delay {
        registry
          .timer(s"${namespace(prefix, classifier)}.abnormal-terminations")
          .update(elapsed, TimeUnit.NANOSECONDS)
      }

      private def namespace(prefix: String, classifier: Option[String]): String =
        classifier.map(d => s"${prefix}.${d}").getOrElse(s"${prefix}.default")

      private def registerStatusCode(status: Status, elapsed: Long, classifier: Option[String]) =
        (status.code match {
          case hundreds if hundreds < 200 =>
            registry.timer(s"${namespace(prefix, classifier)}.1xx-responses")
          case twohundreds if twohundreds < 300 =>
            registry.timer(s"${namespace(prefix, classifier)}.2xx-responses")
          case threehundreds if threehundreds < 400 =>
            registry.timer(s"${namespace(prefix, classifier)}.3xx-responses")
          case fourhundreds if fourhundreds < 500 =>
            registry.timer(s"${namespace(prefix, classifier)}.4xx-responses")
          case _ => registry.timer(s"${namespace(prefix, classifier)}.5xx-responses")
        }).update(elapsed, TimeUnit.NANOSECONDS)

    }

  private def requestTimer(method: Method): String = method match {
    case Method.GET => "get-requests"
    case Method.POST => "post-requests"
    case Method.PUT => "put-requests"
    case Method.HEAD => "head-requests"
    case Method.MOVE => "move-requests"
    case Method.OPTIONS => "options-requests"
    case Method.TRACE => "trace-requests"
    case Method.CONNECT => "connect-requests"
    case Method.DELETE => "delete-requests"
    case _ => "other-requests"
  }
  
}

