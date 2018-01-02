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
import com.typesafe.sbt.packager.MappingsHelper.{directory,contentOf}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

object LuceneIndexCreator extends IndexBuilder {

  val indexBuild = TaskKey[Int]("index-build")
  val indexClean = TaskKey[Int]("index-clean")

  import Append._

  val indexSettings = Seq(
    indexBuild := {
  //    buildIndex()
      0
    },
    indexClean := {
//      clean()
      0
    },

    // (compile in Compile) := ((compile in Compile) dependsOn indexBuild).value,

    resourceGenerators in Compile += Def.task {
      val file = (resourceManaged in Compile).value / "conf" / "index" / "sic8"
      buildIndex(file.toPath)

      // TODO see if this helps with the packaging 'sbt clean dist-tgz' doesn't package the index correctly
      // it's not been copied from target/resource_managed/main to target/classes
      val files = Seq(file) ++ file.listFiles()
      log.debug(s"Index files to be copied by resource generator ${files.mkString(",")}")
      files
    },
//    extraFiles += ((resourceManaged in Compile).value / "conf" / "index" / "sic8" / "*").get,
//    mappings in config("universal") += {
//      ((resourceManaged in Compile).value / "conf" / "index" ) -> "conf/index"
//    },

//    mappings in Universal ++= directory((resourceManaged in Compile).value / "conf" / "index"),
    mappings in Universal ++= contentOf((resourceManaged in Compile).value),

    // clean the old location where indexes were stored
    cleanFiles += baseDirectory { base => base / "conf"/ "index" }.value
  )

//  val indexSic8Path: Path = (resourceManaged in Compile).value.toPath.resolve("index/sic8")
}

trait IndexBuilder {

  val log = ConsoleLogger()
  private val fs: FileSystem = FileSystems.getDefault
//  val rootPath: Path = fs.getPath("conf", "index")
//  val indexSic8Path: Path = rootPath.resolve("sic8")

//  val indexSic8Path: Path

  val industryCodeMapping = Map.apply(
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

  def clean(rootPath: Path) {
    FileUtils.deleteDirectory(rootPath.toFile)
  }

  def buildIndex(indexSic8Path: Path): Directory = {

    val fileSicPipe = "conf/sic-codes.txt"

    val index: Directory = new NIOFSDirectory(indexSic8Path);

    if( index.listAll().size == 0 ) {

//      clean(indexSic8Path) // TODO - ideally could check before building on compile

      // TODO clean out the index directory first
      // TODO only build if out of date or missing

      log.info(s"Building new index into ${indexSic8Path.toAbsolutePath}")

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

      def addDoc(w: IndexWriter, code: String, description: String) {
        val doc = new Document
        doc.add(new StringField("code8", code, Field.Store.YES))
        doc.add(new TextField("description", description, Field.Store.YES))
        doc.add(new SortedSetDocValuesFacetField("sector", returnIndustrySector(code)))
        w.addDocument(facetConfig.build(doc))
      }

      val w = new IndexWriter(index, config);

      import scala.io.Source
      val source = Source.fromFile(fileSicPipe)
      for (line <- source.getLines()) {
        val split = line.split("\\|")
        val code = split(0)
        val desc = split(1)
        addDoc(w, code, desc)
      }
      source.close()

      val numDocs = w.numDocs()
      w.commit() // Should flush the contents
      w.close()

      log.info(s"Index successfully built, $numDocs in the index (took ${System.currentTimeMillis - startTime}ms).")
      Thread.sleep(1000)
      log.info(s"Finished 1a second wait building index.")
      Thread.sleep(1000)
      log.info(s"Finished 1b second wait building index.")
      Thread.sleep(1000)
      log.info(s"Finished 1c second wait building index.")
      Thread.sleep(1000)
      log.info(s"Finished 1d second wait building index.")
      Thread.sleep(1000)
      log.info(s"Finished 1e second wait building index.")
    }
    index
  }

  def returnIndustrySector(sicCode: String): String = {
    val firstTwoChars = sicCode.substring(0,2)
    industryCodeMapping.getOrElse(firstTwoChars,
      throw new Exception(s"Industry code for sic-code $sicCode does not exist")
    )
  }
}
