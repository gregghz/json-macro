This project is an attempt at a scalameta macro for play-json that avoids the overhead of play-json's functional syntax (see: https://www.lucidchart.com/techblog/2016/08/29/speeding-up-restful-services-in-play-framework/).

In contrast with Play's `Json.format[A]` macro, json-macro has to use an annotation (scalameta does not yet support def macros):

    @JsonRecord
    case class Thing(a: String)
