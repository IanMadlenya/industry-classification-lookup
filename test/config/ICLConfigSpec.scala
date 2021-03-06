/*
 * Copyright 2018 HM Revenue & Customs
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

package config

import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.{Configuration, Environment}
import play.api.Mode.Mode
import play.api.libs.json.{JsValue, Json}
import org.mockito.ArgumentMatchers.{eq => eqTo}

class ICLConfigSpec extends PlaySpec with MockitoSugar {
  val mockConfig: Configuration = mock[Configuration]
  val mockEnv: Environment = mock[Environment]

  trait Setup {
    val msConfig: ICLConfig = new ICLConfig {
      override protected def mode: Mode = mockEnv.mode
      override protected def runModeConfiguration: Configuration = mockConfig
    }
  }

  "getConfigObject" should {

    val key = "test"

    val map: Map[String, Any] = Map(
      key -> Map(
        "some" -> "thing",
        "nested" -> Map(
          "nestedKey" -> "nestedValue"
        )
      )
    )

    val confObject = Configuration.from(map).getObject(key)

    "return a config object as json given a key" in new Setup {

      when(mockConfig.getObject(eqTo(key))).thenReturn(confObject)

      val retrievedConfig: JsValue = msConfig.getConfigObject(key)

      val expectedJson: JsValue = Json.parse(
        """
          |{
          | "some":"thing",
          | "nested":{
          |   "nestedKey":"nestedValue"
          | }
          |}
        """.stripMargin)

      retrievedConfig mustBe expectedJson
    }

    "throw an unchecked exception when the config object isn't found using the supplied key" in new Setup {

      when(mockConfig.getObject(eqTo(key))).thenReturn(None)

      val ex: Throwable = intercept[Throwable](msConfig.getConfigObject(key))

      ex.getMessage mustBe s"[Config] Could not find config key $key"
    }
  }
}
