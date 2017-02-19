package com.gregghz.json

import scala.annotation.StaticAnnotation
import scala.meta._
import scala.collection.immutable.Seq
import scala.collection.mutable

object JsonRecord {

  def generateReads(
      name: Type.Name,
      params: Seq[Term.Param],
      overrides: Map[String, String]
  ) = {

    val forLines: Seq[Enumerator.Generator] = params.map { param =>
      val paramTypeStr = param.decltpe.map(_.toString).getOrElse {
        abort(s"${param.name.value} is missing a type annotation")
      }
      val tpe = t"${Type.Name(paramTypeStr)}"
      val pat = Pat.Var.Term(Term.Name(param.name.value))
      val key = overrides.getOrElse(param.name.value, param.name.value)

      param.decltpe match {
        case Some(targ"Option[$t]") =>
          enumerator"$pat <- (json \ $key).validateOpt[${Type.Name(t.toString)}]"
        case Some(t) =>
          enumerator"$pat <- (json \ $key).validate[${Type.Name(t.toString)}]"
      }
    }

    val terms = params.map { param => Term.Name(param.name.value) }

    val tt = q"${Term.Name(name.value)}.apply(..$terms)"
    q"play.api.libs.json.Reads(json => for { ..$forLines } yield $tt)"
  }

  def generateWrites(
      name: Type.Name,
      params: Seq[Term.Param],
      overrides: Map[String, String]
  ) = {
    val pairs = params.map { param =>
      val term = Term.Name(param.name.value)
      val key = overrides.getOrElse(param.name.value, param.name.value)
      q"$key -> value.$term"
    }

    val jsonObj = q"play.api.libs.json.Json.obj(..$pairs)"

    q"play.api.libs.json.Writes(value => $jsonObj)"
  }

  def modifyClass(
    stat: Stat,
    comp: Option[Stat]
  ) = {

    val q"..$mods case class $name(..$rawParams) extends ..$template { ..$body }" = stat

    val (params, overrides) = rawParams.foldLeft((Seq.empty[Term.Param], Map.empty[String, String])) {
      case ((params, overrides), param"$name: $baseType ~ $jsonName") =>
        (params :+ param"$name: $baseType", overrides + (name.toString -> jsonName.toString))
      case ((params, overrides), param) => (params :+ param, overrides)
    }

    val finalCc = q"..$mods case class $name(..$params) extends ..$template { ..$body  }"

    val term = Term.Name(name.value)
    val readsTerm = Pat.Var.Term(Term.Name(s"${name.value}__PlayJsonReads"))
    val writesTerm = Pat.Var.Term(Term.Name(s"${name.value}__PlayJsonWrites"))

    val reads = generateReads(name, params, overrides)
    val writes = generateWrites(name, params, overrides)

    val implicits = Seq(
      q"implicit val $readsTerm: play.api.libs.json.Reads[$name] = $reads",
      q"implicit val $writesTerm: play.api.libs.json.Writes[$name] = $writes"
    )

    val finalComp = comp.map {
      case q"..$mods object $oname { ..$body }" => q"""object $oname {
        ..$body
        ..$implicits
      }"""
      case _ => abort("Invalid companion object")
    }.getOrElse(q"object $term { ..$implicits }")

    q"""
      $finalCc
      $finalComp
    """
  }
}

class JsonRecord extends StaticAnnotation {

  inline def apply(defn: Any): Any = meta {
    import JsonRecord._

    defn match {
      case orignalCc @ q"..$mods case class $name(..$params) extends ..$template { ..$body  }" =>
        modifyClass(orignalCc, None)
      case q"""
        ..$mods case class $cname(..$cparams) extends ..$template { ..$cbody  }
        $comp
      """ =>
        val originalCc = q"..$mods case class $cname(..$cparams) extends ..$template { ..$cbody }"
        modifyClass(originalCc, Some(comp))
      case _ =>
        abort("@JsonRecord must be used with a case class")
    }
  }
}