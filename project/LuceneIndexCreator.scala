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

import java.nio.file.{FileSystem, FileSystems, Path, Paths}

import org.apache.commons.io.FileUtils
import org.apache.lucene.analysis.CharArraySet
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.{Document, Field, StringField, TextField}
import org.apache.lucene.facet.FacetsConfig
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetField
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.store.{Directory, NIOFSDirectory}
import sbt.Keys._
import sbt.{TaskKey, _}
import com.typesafe.sbt.SbtNativePackager.Universal
import com.typesafe.sbt.packager.MappingsHelper.contentOf

object LuceneIndexCreator {

  val log = ConsoleLogger()
  val indexBuild = TaskKey[Seq[File]]("index-build")

  import Append._

  val indexBuildTask = Def.task {
    val root = (resourceManaged in Compile).value / "conf" / "index"
    val file = HMRCSIC8Builder.buildIndex(root)

    val files = Seq(file) ++ file.listFiles()
    log.debug(s"Index files to be copied by resource generator ${files.mkString(",")}")
    files
  }

  val indexSettings = Seq(
    indexBuild := indexBuildTask.value,

    resourceGenerators in Compile += indexBuildTask,

    mappings in Universal <++= (packageBin in Compile, resourceManaged in Compile) map { (_, managed) => {
      contentOf( managed )
    } },

    // clean the old location where indexes were stored
    cleanFiles += baseDirectory { base => base / "conf"/ "index" }.value
  )
}

object HMRCSIC8Builder extends SICIndexBuilder {

  def buildIndex(rootPath: File): File = {
    val name = "hmrc-sic8"
    val sic8Path = rootPath / name

    buildIndexInternal(name, sic8Path.toPath){ addDocument =>
      import scala.io.Source
      var docsAdded = 0
      val fileSicPipe = "conf/sic-codes.txt"
      val source = Source.fromFile(fileSicPipe)
      for (line <- source.getLines()) {
        val split = line.split("\\|")
        val code = split(0)
        val desc = split(1)
        addDocument(SicDocument(code, desc))
        docsAdded+=1
      }
      source.close()

      docsAdded
    }

    sic8Path
  }
}


case class SicDocument(code: String, description: String)

trait SICIndexBuilder extends IndustryCodeMapping {

  type AddDocument = Function1[SicDocument, Boolean]

  val log = ConsoleLogger()
  private val fs: FileSystem = FileSystems.getDefault

  def clean(rootPath: Path) {
    FileUtils.deleteDirectory(rootPath.toFile)
  }

  def buildIndexInternal(name: String, indexSic8Path: Path)(processSource: AddDocument => Int): Directory = {

    val index: Directory = new NIOFSDirectory(indexSic8Path);

    // Only build if out of date or missing
    if( index.listAll().size == 0 ) {

      log.info(s"""Building new index "${name}" into ${indexSic8Path.toAbsolutePath}""")

      val startTime = System.currentTimeMillis()

      // TODO doesn't seem to be working - i.e. to drop out IT
      val stopWords = List(
        "a", "an", "and", "are", "as", "at", "be", "but", "by",
        "for", "if", "in", "into", "is", // "it",
        "no", "not", "of", "on", "or", "such",
        "that", "the", "their", "then", "there", "these",
        "they", "this", "to", "was", "will", "with"
      )
      val stopSet = {
        import scala.collection.JavaConverters._
        new CharArraySet(stopWords.asJava, true);
      }

      val analyzer = new StandardAnalyzer(stopSet);
      val config = new IndexWriterConfig(analyzer);
      val facetConfig = new FacetsConfig()


      def addDoc(w: IndexWriter)(sicDoc: SicDocument) = {
        val doc = new Document
        doc.add(new StringField("code8", sicDoc.code, Field.Store.YES))
        doc.add(new TextField("description", sicDoc.description, Field.Store.YES))
        doc.add(new SortedSetDocValuesFacetField("sector", returnIndustrySector(sicDoc.code)))
        w.addDocument(facetConfig.build(doc))
        true
      }

      val w = new IndexWriter(index, config);

      val numAdded = processSource(addDoc(w)_)

      val numIndexDocs = w.numDocs()

      w.commit() // Should flush the contents
      w.close()

      log.info(s"""Index "${name}" successfully built, $numIndexDocs in the index (adding ${numAdded} took ${System.currentTimeMillis - startTime}ms).""")
    }
    index
  }

}

trait IndustryCodeMapping {
  private val industryCodeMapping = Map.apply(
    "01" -> "A", "02" -> "A", "03" -> "A",
    "05" -> "B", "06" -> "B", "07" -> "B", "08" -> "B", "09" -> "B",
    "10" -> "C", "11" -> "C", "12" -> "C", "13" -> "C", "14" -> "C", "15" -> "C", "16" -> "C", "17" -> "C",
    "18" -> "C", "19" -> "C", "20" -> "C", "21" -> "C", "22" -> "C", "23" -> "C", "24" -> "C", "25" -> "C",
    "26" -> "C", "27" -> "C", "28" -> "C", "29" -> "C", "30" -> "C", "31" -> "C", "32" -> "C", "33" -> "C",
    "35" -> "D",
    "36" -> "E", "37" -> "E", "38" -> "E", "39" -> "E",
    "41" -> "F", "42" -> "F", "43" -> "F",
    "45" -> "G", "46" -> "G", "47" -> "G",
    "49" -> "H", "50" -> "H", "51" -> "H", "52" -> "H", "53" -> "H",
    "55" -> "I", "56" -> "I",
    "58" -> "J", "59" -> "J", "60" -> "J", "61" -> "J", "62" -> "J", "63" -> "J",
    "64" -> "K", "65" -> "K", "66" -> "K",
    "68" -> "L",
    "69" -> "M", "70" -> "M", "71" -> "M", "72" -> "M", "73" -> "M", "74" -> "M", "75" -> "M",
    "77" -> "N", "78" -> "N", "79" -> "N", "80" -> "N", "81" -> "N", "82" -> "N",
    "84" -> "O",
    "85" -> "P",
    "86" -> "Q", "87" -> "Q", "88" -> "Q",
    "90" -> "R", "91" -> "R", "92" -> "R", "93" -> "R",
    "94" -> "S", "95" -> "S", "96" -> "S",
    "97" -> "T", "98" -> "T",
    "99" -> "U"
  )

  def returnIndustrySector(sicCode: String): String = {
    val firstTwoChars = sicCode.substring(0,2)
    industryCodeMapping.getOrElse(firstTwoChars,
      throw new Exception(s"Industry code for sic-code $sicCode does not exist")
    )
  }
}
