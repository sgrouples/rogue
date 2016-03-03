package com.foursquare.rogue

import java.util.concurrent.ConcurrentHashMap

import com.foursquare.index.{IndexedRecord, UntypedMongoIndex}
import com.foursquare.rogue.MongoHelpers.MongoSelect
import com.mongodb.async.client.{MongoClient, MongoCollection, MongoDatabase}
import com.mongodb.{DBObject, MongoException}
import net.liftweb.mongodb.record.{MongoMetaRecord, MongoRecord}
import net.liftweb.util.ConnectionIdentifier
import org.bson.Document

//maye it should live in Lift
object MongoAsync {

  /*
  * HashMap of Mongo instance and db name tuples, keyed by ConnectionIdentifier
  */
  private val dbs = new ConcurrentHashMap[ConnectionIdentifier, (MongoClient, String)]

  /**
    * Define a Mongo db using a MongoClient instance.
    */
  def defineDb(name: ConnectionIdentifier, mngo: MongoClient, dbName: String) {
    dbs.put(name, (mngo, dbName))
  }

  /*
  * Get a DB reference
  */
  def getDb(name: ConnectionIdentifier): Option[MongoDatabase] = dbs.get(name) match {
    case null => None
    case (mngo, db) => Some(mngo.getDatabase(db))
  }


  def useSession[T](ci: ConnectionIdentifier)(f: (MongoDatabase) => T): T = {
    val db = getDb(ci) match {
      case Some(mongo) => mongo
      case _ => throw new MongoException("Mongo not found: "+ci.toString)
    }
    f(db)
  }

}


object LiftAsyncDBCollectionFactory extends AsyncDBCollectionFactory[MongoRecord[_] with MongoMetaRecord[_]] {
  override def getDBCollection[M <: MongoRecord[_] with MongoMetaRecord[_]](query: Query[M, _, _]): MongoCollection[Document] = {
    MongoAsync.useSession(query.meta.connectionIdentifier) { db =>
      db.getCollection(query.collectionName)
    }
  }

  override def getPrimaryDBCollection[M <: MongoRecord[_] with MongoMetaRecord[_]](query: Query[M, _, _]): MongoCollection[Document] = {
    MongoAsync.useSession(query.meta /* TODO: .master*/ .connectionIdentifier) { db =>
      db.getCollection(query.collectionName)
    }
  }

  override def getInstanceName[M <: MongoRecord[_] with MongoMetaRecord[_]](query: Query[M, _, _]): String = {
    query.meta.connectionIdentifier.toString
  }

  /**
    * Retrieves the list of indexes declared for the record type associated with a
    * query. If the record type doesn't declare any indexes, then returns None.
    *
    * @param query the query
    * @return the list of indexes, or an empty list.
    */
  override def getIndexes[M <: MongoRecord[_] with MongoMetaRecord[_]](query: Query[M, _, _]): Option[List[UntypedMongoIndex]] = {
    val queryMetaRecord = query.meta
    if (queryMetaRecord.isInstanceOf[IndexedRecord[_]]) {
      Some(queryMetaRecord.asInstanceOf[IndexedRecord[_]].mongoIndexList)
    } else {
      None
    }
  }
}

class LiftAsyncAdapter(dbCollectionFactory: AsyncDBCollectionFactory[MongoRecord[_] with MongoMetaRecord[_]]) extends MongoAsyncJavaDriverAdapter(dbCollectionFactory)


object LiftAsyncAdapter extends LiftAsyncAdapter(LiftAsyncDBCollectionFactory)


class LiftAsyncQueryExecutor(override val adapter: MongoAsyncJavaDriverAdapter[MongoRecord[_] with MongoMetaRecord[_]]) extends AsyncQueryExecutor[MongoRecord[_] with MongoMetaRecord[_]] {
  override def defaultWriteConcern = QueryHelpers.config.defaultWriteConcern

  override lazy val optimizer = new QueryOptimizer

  override protected def serializer[M <: MongoRecord[_] with MongoMetaRecord[_], R](
                                                                                     meta: M,
                                                                                     select: Option[MongoSelect[M, R]]
                                                                                   ): RogueSerializer[R] = {
    new RogueSerializer[R] {
      override def fromDBObject(dbo: DBObject): R = select match {
        case Some(MongoSelect(Nil, transformer)) =>
          // A MongoSelect clause exists, but has empty fields. Return null.
          // This is used for .exists(), where we just want to check the number
          // of returned results is > 0.
          transformer(null)
        case Some(MongoSelect(fields, transformer)) =>
          val inst = meta.createRecord.asInstanceOf[MongoRecord[_]]

          LiftQueryExecutorHelpers.setInstanceFieldFromDbo(inst, dbo, "_id")

          val values =
            fields.map(fld => {
              val valueOpt = LiftQueryExecutorHelpers.setInstanceFieldFromDbo(inst, dbo, fld.field.name)
              fld.valueOrDefault(valueOpt)
            })

          transformer(values)
        case None =>
          meta.fromDBObject(dbo).asInstanceOf[R]
      }
    }
  }
}

object LiftAsyncQueryExecutor extends LiftAsyncQueryExecutor(LiftAsyncAdapter)