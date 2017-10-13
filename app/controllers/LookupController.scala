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

package controllers

import javax.inject.{Inject, Singleton}

import play.api.mvc.{Action, AnyContent}
import services.LookupService
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.Future

@Singleton
class LookupControllerImpl @Inject()(val lookupService: LookupService) extends LookupController

trait LookupController extends BaseController {

  val lookupService: LookupService

  def lookup(sicCode: String): Action[AnyContent] = Action.async{
    implicit request =>
      lookupService.lookup(sicCode) match {
        case Some(sicCodeDescription) => Future.successful(Ok(sicCodeDescription))
        case None => Future.successful(NotFound)
      }
  }
}