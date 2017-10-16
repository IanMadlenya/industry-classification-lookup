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

import org.apache.lucene.store.{Directory, NIOFSDirectory, RAMDirectory}
import sbt.TaskKey
import sbt._
import sbt.Keys._
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.{Document, Field, StringField, TextField}
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import java.nio.file.{FileSystems, Path}

object LuceneIndexCreator extends IndexBuilder {

  val indexBuild = TaskKey[Int]("index-build")

  val indexSettings = Seq(
    indexBuild := {
      buildIndex()
      0
    },

    (compile in Compile) <<= (compile in Compile) dependsOn indexBuild
  )
}

trait IndexBuilder {

  val log = ConsoleLogger()

  def buildIndex() = {

    // TODO clean out the index directory first
    // TODO only build if out of date or missing
    // TODO add conf/index - or whatever it becomes, into .gitignore

    val path: Path = FileSystems.getDefault().getPath("conf", "index")

    log.info(s"Building new index into ${path.toAbsolutePath}")

    val startTime = System.currentTimeMillis()

    val index: Directory = new NIOFSDirectory(path);

    val analyzer = new StandardAnalyzer();
    val config = new IndexWriterConfig(analyzer);
    def addDoc(w: IndexWriter, title: String, isbn: String) {
      val doc = new Document
      doc.add(new TextField("title", title, Field.Store.YES))
      doc.add(new StringField("isbn", isbn, Field.Store.YES))
      w.addDocument(doc)
    }

    val w = new IndexWriter(index, config);
    addDoc(w, "Lucene in Action", "193398817");
    addDoc(w, "Lucene for Dummies", "55320055Z");
    addDoc(w, "Managing Gigabytes", "55063554A");
    addDoc(w, "The Art of Computer Science", "9900333X");
    w.close();

    log.info(s"Index successfully built (took ${System.currentTimeMillis-startTime}ms).")
    index
  }


}
