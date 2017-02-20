# json-macro

json-macro is an attempt at a scalameta macro for play-json that avoids the overhead of play-json's functional syntax. See [this blog post](https://www.lucidchart.com/techblog/2016/08/29/speeding-up-restful-services-in-play-framework/) for details.

Compatibility is currently tested against play-json 2.6.0-M1. That means it is compatible with play-json 2.5.x as well but that is mostly incidental. Once 2.6.x becomes final I plan to support the latest stable release of play-json moving forward. Any version that has the same Reads/Writes API should automatically be supported by json-macro.

## Install

Add the follow to your build.sbt:

    resolvers += Resolver.bintrayRepo("gregghz", "gregghz")

    addCompilerPlugin("org.scalameta" % "paradise" % "3.0.0-beta4" cross CrossVersion.full)

    libraryDependencies += "com.gregghz" %% "json-macro" % "0.1.0"

## Usage

In contrast with Play's `Json.format[A]` macro, json-macro has to use an annotation until [#160](https://github.com/scalameta/scalameta/issues/160) is resolved:

    import com.gregghz.json._

    @JsonRecord
    case class Thing(a: String)

This will expand into roughly the following code:

    case class Thing(a: String)
    object Thing {
      implicit val reads: Reads[Thing] = Reads[Thing] { json =>
        for {
          a <- (json \ "a").validate[String]
        } yield Thing(a)
      }

      implicit val writes: Writes[Thing] = Writes[Thing] { obj =>
        Json.obj(
          "a" -> obj.a
        )
      }
    }

You can also override the keys used in the serialized json:

    import com.gregghz.json._

    @JsonRecord
    case class Thing(a: String ~ hello)

This will expand into the following:

    case class Thing(a: String)
    object Thing {
      implicit val reads: Reads[Thing] = Reads[Thing] { json =>
        for {
          a <- (json \ "hello").validate[String]
        } yield Thing(a)
      }

      implicit val writes: Writes[Thing] = Writes[Thing] { obj =>
        Json.obj(
          "hello" -> obj.a
        )
      }
    }
