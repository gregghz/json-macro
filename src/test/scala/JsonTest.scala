package com.gregghz.json

import org.specs2.mutable._
import play.api.libs.json._

class JsonTest extends Specification {

  "@JsonRecord" should {
    "generate Reads and Writes" in {
      @JsonRecord
      case class SimpleClass(
        string: String,
        int: Option[Int]
      )

      val startingJson = Json.obj("string" -> "hello", "int" -> 10)

      val result = startingJson.as[SimpleClass]
      result.string mustEqual "hello"
      result.int mustEqual Some(10)

      val json = Json.toJson(SimpleClass("hello", Some(10)))
      json mustEqual startingJson
    }

    "preserve a predefined companion object" in {
      @JsonRecord
      case class SimpleClass(string: String, int: Int)
      object SimpleClass {
        def f(): String = "hello"
      }

      val startingJson = Json.obj("string" -> "hello", "int" -> 10)

      val result = startingJson.as[SimpleClass]
      result.string mustEqual "hello"
      result.int mustEqual 10

      val json = Json.toJson(SimpleClass("hello", 10))
      json mustEqual startingJson

      SimpleClass.f() mustEqual "hello"
    }

    "override field names" in {
      @JsonRecord
      case class SimpleClass(
        string: String ~ str,
        int: Int ~ `complex-value_123!!!`
      )

      val startingJson = Json.obj("str" -> "hello", "complex-value_123!!!" -> 10)

      val result = startingJson.as[SimpleClass]
      result.string mustEqual "hello"
      result.int mustEqual 10

      val json = Json.toJson(SimpleClass("hello", 10))
      json mustEqual startingJson
    }
  }
}