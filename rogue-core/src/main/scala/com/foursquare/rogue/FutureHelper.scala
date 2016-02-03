package com.foursquare.rogue

import com.mongodb.async.SingleResultCallback

import scala.concurrent.{Future, Promise}

private[rogue] object FutureHelper {

  def forSingleResultCallback[T]: (SingleResultCallback[T], Future[T]) = {
    val p = Promise[T]
    val cb: SingleResultCallback[T] = new SingleResultCallback[T] {
      override def onResult(t: T, throwable: Throwable): Unit = {
        if(throwable eq null) p.success(t)
        else p.failure(throwable)
      }
    }
    (cb, p.future)
  }
}
