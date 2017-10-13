/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lucene

import uk.gov.hmrc.play.test.UnitSpec
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.{Document, Field, StringField, TextField}
import org.apache.lucene.index.{DirectoryReader, IndexReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.{Directory, RAMDirectory}
import org.apache.lucene.queryparser.classic.QueryParser


class Lucene101Spec extends UnitSpec {

 "Wibble" should {

   val analyzer = new StandardAnalyzer();
   val config = new IndexWriterConfig(analyzer);

   def buildIndex() = {
     def addDoc(w: IndexWriter, title: String, isbn: String) {
       val doc = new Document
       doc.add(new TextField("title", title, Field.Store.YES))
       doc.add(new StringField("isbn", isbn, Field.Store.YES))
       w.addDocument(doc)
     }

     val index: Directory = new RAMDirectory();

     val w = new IndexWriter(index, config);
     addDoc(w, "Lucene in Action", "193398817");
     addDoc(w, "Lucene for Dummies", "55320055Z");
     addDoc(w, "Managing Gigabytes", "55063554A");
     addDoc(w, "The Art of Computer Science", "9900333X");
     w.close();

     index
   }

    "foo" in {
      val index = buildIndex()

      val reader: IndexReader = DirectoryReader.open(index)
      val searcher = new IndexSearcher(reader)

      val qp = new QueryParser("title", analyzer)

      val result = searcher.search(qp.parse("Lucene^2 OR Science"), 5)

      result.totalHits shouldBe 3

      result.scoreDocs.toSeq map {
        result =>
          val doc = searcher.doc(result.doc)
          val title = doc.getFields("title").head.stringValue()
          val isbn = doc.getFields("isbn").head.stringValue()
          (result.score, isbn, title)
      } shouldBe "xxx"
    }
  }


}
