package org.ziverge.ecosystem

import com.raquo.domtypes.generic.nodes.Node
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.laminar.api.L.*
import org.scalajs.dom
import com.raquo.laminar.builders
import com.raquo.laminar.nodes.ParentNode.Base
//import com.raquo.laminar.api.Laminar.Mod

object Bulma:
  val size5: Mod[ReactiveHtmlElement[_]] = cls := "is-size-5"
  val boxed: Mod[ReactiveHtmlElement[_]] = cls := "box"

//  def size5(others: Mod[ReactiveHtmlElement[_]]*): Mod[ReactiveHtmlElement[_]] =
//    new Modifier[ReactiveHtmlElement[_]] {
//      override def apply(element: ReactiveHtmlElement[_]): Unit = element.amend(cls := "is-size-5").amend(others*)
//    }
    //cls := "is-size-5" ++ Seq(others)

//  def boxed(others: Mod[ReactiveHtmlElement[_]]*): Mod[ReactiveHtmlElement[_]] =
//    new Modifier[ReactiveHtmlElement[_]] {
//      override def apply(element: ReactiveHtmlElement[_]): Unit = element.amend(cls := "box").amend(others*)
//    }
//
//  def p3(others: Mod[ReactiveHtmlElement[_]]*): Mod[ReactiveHtmlElement[_]] =
//    new Modifier[ReactiveHtmlElement[_]] {
//      override def apply(element: ReactiveHtmlElement[_]): Unit = element.amend(cls := "p-3").amend(others*)
//    }

  val p3: Mod[ReactiveHtmlElement[_]] =
    new Modifier[ReactiveHtmlElement[_]] {
      override def apply(element: ReactiveHtmlElement[_]): Unit = element.amend(cls := "p-3 yyy")
    }

  extension(
             el: Mod[ReactiveHtmlElement[_]]
           )
    def boxed: Mod[ReactiveHtmlElement[_]] =
      new Modifier[ReactiveHtmlElement[_]] {
        override def apply(element: ReactiveHtmlElement[_]): Unit = element
          .amend(el)
          .amend(cls := "box")
      }

    def p3: Mod[ReactiveHtmlElement[_]] =
      new Modifier[ReactiveHtmlElement[_]] {
        override def apply(element: ReactiveHtmlElement[_]): Unit = element
          .amend(el)
          .amend(cls := "p3")
      }

    def size5: Mod[ReactiveHtmlElement[_]] =
      new Modifier[ReactiveHtmlElement[_]] {
        override def apply(element: ReactiveHtmlElement[_]): Unit = element
          .amend(el)
          .amend(cls := "is-size-5")
      }

//  val box = cls := "box p-3",


//  extension  (element: Mod[ReactiveHtmlElement[_]] => ReactiveHtmlElement[_])
//    def boxed =
//      element(cls := "box p-3")

//extension  (element:
//              Seq[
//                com.raquo.domtypes.generic.Modifier[_] |
//                Modifier[_] | Setter[_] | Image
//              ]  => com.raquo.laminar.nodes.ReactiveHtmlElement[_]
//             )
//    def boxed =
//      (otherArgs: Seq[com.raquo.domtypes.generic.Modifier[_] | Modifier[_] | Setter[_] | Image]) => element(Seq(cls := "box p-3") ++ otherArgs).apply

