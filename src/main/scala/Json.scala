package com.gregghz.json

import scala.annotation.StaticAnnotation
import scala.meta._
import scala.collection.immutable.Seq

object JsonRecord {

  def generateReads(name: Type.Name, params: Seq[Term.Param]) = {

    val forLines: Seq[Enumerator.Generator] = params.map { param =>
      val paramTypeStr = param.decltpe.map(_.toString).getOrElse {
        abort(s"${param.name.value} is missing a type annotation")
      }
      val tpe = t"${Type.Name(paramTypeStr)}"
      val pat = Pat.Var.Term(Term.Name(param.name.value))
      enumerator"$pat <- (json \ ${param.name.value}).validate[$tpe]"
    }

    val terms = params.map { param => Term.Name(param.name.value) }

    val tt = q"${Term.Name(name.value)}.apply(..$terms)"
    q"play.api.libs.json.Reads(json => for { ..$forLines } yield $tt)"
  }

  def generateWrites(name: Type.Name, params: Seq[Term.Param]) = {
    val pairs = params.map { param =>
      val term = Term.Name(param.name.value)
      q"${param.name.value} -> value.$term"
    }

    val jsonObj = q"play.api.libs.json.Json.obj(..$pairs)"

    q"play.api.libs.json.Writes(value => $jsonObj)"
  }

  def modifyClass(
    mods: Seq[Mod],
    name: Type.Name,
    params: Seq[Term.Param],
    body: Seq[Stat],
    template: Seq[Ctor.Call],
    comp: Option[Stat]
  ) = {
    val term = Term.Name(name.value)
    val readsTerm = Pat.Var.Term(Term.Name(s"${name.value}__PlayJsonReads"))
    val writesTerm = Pat.Var.Term(Term.Name(s"${name.value}__PlayJsonWrites"))

    val implicits = Seq(
      q"implicit val $readsTerm: play.api.libs.json.Reads[$name] = ${generateReads(name, params)}",
      q"implicit val $writesTerm: play.api.libs.json.Writes[$name] = ${generateWrites(name, params)}"
    )

    val finalComp = comp.map {
      case q"..$mods object $oname { ..$body }" => q"""object $oname {
        ..$body
        ..$implicits
      }"""
      case _ => abort("Invalid companion object")
    }.getOrElse(q"object $term { ..$implicits }")

    q"""
      ..$mods case class $name(..$params) extends ..$template { .. $body  }
      $finalComp
    """
  }
}

class JsonRecord extends StaticAnnotation {

  inline def apply(defn: Any): Any = meta {
    import JsonRecord._
    defn match {
      case q"""..$mods case class $name(..$params) extends ..$template { ..$body  }""" =>
        modifyClass(mods, name, params, body, template, None)
      case q"""
        ..$mods case class $cname(..$cparams) extends ..$template { ..$cbody  }
        $comp
      """ =>
        modifyClass(mods, cname, cparams, cbody, template, Some(comp))
      case _ =>
        abort("@JsonRecord must be used with a case class")
    }
  }
}