package com.foursquare.rogue

import java.util

import com.foursquare.index.UntypedMongoIndex
import com.foursquare.rogue.MongoHelpers.MongoBuilder._
import com.foursquare.rogue.QueryHelpers._
import com.mongodb.client.result.{UpdateResult, DeleteResult}
import com.mongodb.{BasicDBObject, WriteConcern, DBObject, ReadPreference}
import com.mongodb.async.SingleResultCallback
import com.mongodb.async.client.{DistinctIterable, MongoCollection}
import com.mongodb.client.model.{UpdateOptions, CountOptions}
import org.bson.Document
import org.bson.conversions.Bson
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.concurrent.{Future, Promise}

trait AsyncDBCollectionFactory[MB] {
  def getDBCollection[M <: MB](query: Query[M, _, _]): MongoCollection[Document]

  def getPrimaryDBCollection[M <: MB](query: Query[M, _, _]): MongoCollection[Document]

  def getInstanceName[M <: MB](query: Query[M, _, _]): String

  // A set of of indexes, which are ordered lists of field names
  def getIndexes[M <: MB](query: Query[M, _, _]): Option[List[UntypedMongoIndex]]

}

class PromiseCallbackBridge[T] extends SingleResultCallback[T] {
  val promise = Promise[T]()

  override def onResult(result: T, t: Throwable): Unit = {
    if (result != null) promise.success(result)
    else promise.failure(t)
  }

  def future = promise.future
}

//specialized for Long to avoid conversion quirks
class PromiseLongCallbackBridge extends SingleResultCallback[java.lang.Long] {
  val promise = Promise[Long]()
  override def onResult(result: java.lang.Long, t: Throwable): Unit = {
    if (result != null) promise.success(result)
    else promise.failure(t)
  }
  def future = promise.future
}

class PromiseArrayListAdapter[R] extends SingleResultCallback[java.util.Collection[R]] {
  val coll = new util.ArrayList[R]()
  private[this] val p = Promise[Seq[R]]
  //coll == result - by contract
  override def onResult(result: util.Collection[R], t: Throwable): Unit = {
    if (result != null) p.success(coll)
    else p.failure(t)
  }
  def future = p.future
}

class MongoAsyncJavaDriverAdapter[MB](dbCollectionFactory: AsyncDBCollectionFactory[MB]) {

  def count[M <: MB](query: Query[M, _, _], readPreference: Option[ReadPreference]): Future[Long] = {
    val queryClause = transformer.transformQuery(query)
    validator.validateQuery(queryClause, dbCollectionFactory.getIndexes(queryClause))
    val condition: Bson = buildCondition(queryClause.condition)
    //we don't care for skip, limit in count - maybe we deviate from original, but it makes no sense anyways
    val coll = dbCollectionFactory.getDBCollection(query)
    val callback = new PromiseLongCallbackBridge()
    if(queryClause.lim.isDefined || queryClause.sk.isDefined) {
      val options = new CountOptions()
      queryClause.lim.map(options.limit(_))
      queryClause.sk.map(options.skip(_))
      coll.count(condition, options, callback)
    } else {
      coll.count(condition, callback)
    }
    callback.future
  }

  /*
  def countDistinct[M <: MB](query: Query[M, _, _],
                             key: String,
                             readPreference: Option[ReadPreference]): Long = {
    val queryClause = transformer.transformQuery(query)
    validator.validateQuery(queryClause, dbCollectionFactory.getIndexes(queryClause))
    val cnd = buildCondition(queryClause.condition)
    val coll = dbCollectionFactory.getDBCollection(query)
    val distIterable: DistinctIterable[Document] = coll.distinct(key, cnd, classOf[Document])
    //what to do next? - we should just iterate and count...
    // TODO: fix this so it looks like the correct mongo shell command
    val description = buildConditionString("distinct", query.collectionName, queryClause)

    runCommand(description, queryClause) {
      val coll = dbCollectionFactory.getDBCollection(query)
      coll.distinct(key, cnd, readPreference.getOrElse(coll.find().getReadPreference)).size()
    }
  }
  */
/*
  def distinct[M <: MB, R <: Document](query: Query[M, _, _],
                           key: String,
                           readPreference: Option[ReadPreference]): Future[Seq[R]] = {
    val queryClause = transformer.transformQuery(query)
    validator.validateQuery(queryClause, dbCollectionFactory.getIndexes(queryClause))
    val cnd = buildCondition(queryClause.condition)
    val coll = dbCollectionFactory.getDBCollection(query)
    val distIterable: DistinctIterable[Document] = coll.distinct(key, cnd, classOf[Document])
    //    <A extends Collection<? super TResult>> void into(A target, SingleResultCallback<A> callback);
    val pa = new PromiseArrayListAdapter[R]()
    distIterable.into(pa.coll, pa)
    pa.future
  }
*/

  def find[M <: MB, R](query: Query[M, _, _], serializer: RogueSerializer[R]): Future[Seq[R]] = {
    val queryClause = transformer.transformQuery(query)
    validator.validateQuery(queryClause, dbCollectionFactory.getIndexes(queryClause))
    val cnd = buildCondition(queryClause.condition)
    val coll = dbCollectionFactory.getDBCollection(query)
    //check if serializer will work - quite possible that no, and separate mapper from Document -> R will be needed
    val adaptedSerializer = new com.mongodb.Function[Document,R]{
      override def apply(d: Document):R = serializer.fromDBObject(d.asInstanceOf[DBObject])
    }
    val pa = new PromiseArrayListAdapter[R]()
    coll.find(cnd).map(adaptedSerializer).into(pa.coll, pa)
    pa.future
  }


  def delete[M <: MB](query: Query[M, _, _],
                      writeConcern: WriteConcern): Future[Unit] = {
    val queryClause = transformer.transformQuery(query)
    validator.validateQuery(queryClause, dbCollectionFactory.getIndexes(queryClause))
    val cnd = buildCondition(queryClause.condition)
    val coll = dbCollectionFactory.getPrimaryDBCollection(query)
    val p = Promise[Unit]
    coll.deleteMany(cnd, new SingleResultCallback[DeleteResult] {
      override def onResult(result: DeleteResult, t: Throwable): Unit = {
        if(result != null) p.success(())
        else p.failure(t)
      }
    })
    p.future
  }

  def modify[M <: MB](mod: ModifyQuery[M, _],
                      upsert: Boolean,
                      multi: Boolean,
                      writeConcern: WriteConcern): Future[Unit] = {
    val modClause = transformer.transformModify(mod)
    validator.validateModify(modClause, dbCollectionFactory.getIndexes(modClause.query))
    val p = Promise[Unit]
    if (!modClause.mod.clauses.isEmpty) {
      val q = buildCondition(modClause.query.condition)
      val m = buildModify(modClause.mod).asInstanceOf[BasicDBObject]
      val coll = dbCollectionFactory.getPrimaryDBCollection(modClause.query)

      coll.updateMany(q,m, new UpdateOptions(), new SingleResultCallback[UpdateResult] {
        override def onResult(result: UpdateResult, t: Throwable): Unit = {
          if(result != null) p.success(())
          else p.failure(t)
        }
      })
    } else p.success(())
    //else clause = no modify, automatic success ... strange but true
    p.future
  }



}
