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
  val hasTextGrey: Mod[ReactiveHtmlElement[_]] = cls := "has-text-grey-dark"
  val column: Mod[ReactiveHtmlElement[_]] = cls := "column"
  val columns: Mod[ReactiveHtmlElement[_]] = cls := "columns"

  val button: Mod[ReactiveHtmlElement[_]] = cls := "button"
  val isInfo: Mod[ReactiveHtmlElement[_]] = cls := "is-info"
  val m3: Mod[ReactiveHtmlElement[_]] = cls := "m-3"
  val p3: Mod[ReactiveHtmlElement[_]] = cls := "p-3"

  val isHidden: Mod[ReactiveHtmlElement[_]] = cls := "is-hidden"
  val isFullWidth: Mod[ReactiveHtmlElement[_]] = cls := "is-fullwidth"
  val isNormal: Mod[ReactiveHtmlElement[_]] = cls := "is-normal"
  val isHorizontal: Mod[ReactiveHtmlElement[_]] = cls := "is-horizontal"

  val hasTextWeightBold: Mod[ReactiveHtmlElement[_]] = cls := "has-text-weight-bold"
  val hasBackgroundSuccess: Mod[ReactiveHtmlElement[_]] = cls := "has-background-success"
  val hasBackgroundWarning: Mod[ReactiveHtmlElement[_]] = cls := "has-background-warning"

  val content: Mod[ReactiveHtmlElement[_]] = cls := "content"
  val container: Mod[ReactiveHtmlElement[_]] = cls := "container"
  val card: Mod[ReactiveHtmlElement[_]] = cls := "card"
  val cardContent: Mod[ReactiveHtmlElement[_]] = cls := "card-content"
  val cardHeader: Mod[ReactiveHtmlElement[_]] = cls := "card-header"
  val cardHeaderTitle: Mod[ReactiveHtmlElement[_]] = cls := "card-header-title"
  val cardHeaderIcon: Mod[ReactiveHtmlElement[_]] = cls := "card-header-icon"
  val cardToggle: Mod[ReactiveHtmlElement[_]] = cls := "card-toggle"

  val field: Mod[ReactiveHtmlElement[_]] = cls := "field"
  val fieldLabel: Mod[ReactiveHtmlElement[_]] = cls := "field-label"
  val fieldBody: Mod[ReactiveHtmlElement[_]] = cls := "field-body"

  val icon: Mod[ReactiveHtmlElement[_]] = cls := "icon"

  val control: Mod[ReactiveHtmlElement[_]] = cls := "control"
  val labelB: Mod[ReactiveHtmlElement[_]] = cls := "label"
  val inputB: Mod[ReactiveHtmlElement[_]] = cls := "input"

  val fa: Mod[ReactiveHtmlElement[_]] = cls := "fa"
  val faAngleDown: Mod[ReactiveHtmlElement[_]] = cls := "fa-angle-down"

  val TODO: Mod[ReactiveHtmlElement[_]] = cls := "TODO"

  extension(
             el: Mod[ReactiveHtmlElement[_]]
           )
    def TODO: Mod[ReactiveHtmlElement[_]] =
      newMod("TODO")

    def container: Mod[ReactiveHtmlElement[_]] =
      newMod("container")

    def card: Mod[ReactiveHtmlElement[_]] =
      newMod("card")

    def cardContent: Mod[ReactiveHtmlElement[_]] =
      newMod("card-content")

    def cardHeader: Mod[ReactiveHtmlElement[_]] =
      newMod("card-header")
    def cardHeaderTitle: Mod[ReactiveHtmlElement[_]] =
      newMod("card-header-title")
    def cardHeaderIcon: Mod[ReactiveHtmlElement[_]] =
      newMod("card-header-icon")
    def cardToggle: Mod[ReactiveHtmlElement[_]] =
      newMod("card-toggle")

    def content: Mod[ReactiveHtmlElement[_]] =
      newMod("content")

    def hasTextWeightBold: Mod[ReactiveHtmlElement[_]] =
      newMod("has-text-weight-bold")
    def hasBackgroundSuccess: Mod[ReactiveHtmlElement[_]] =
      newMod("has-background-success")
    def hasBackgroundWarning: Mod[ReactiveHtmlElement[_]] =
      newMod("has-background-warning")

    def isHidden: Mod[ReactiveHtmlElement[_]] =
      newMod("is-hidden")
    def isFullWidth: Mod[ReactiveHtmlElement[_]] =
      newMod("is-fullwidth") // TODO Is this value bogus?
    def isNormal: Mod[ReactiveHtmlElement[_]] =
      newMod("is-normal")
    def isHorizontal: Mod[ReactiveHtmlElement[_]] =
      newMod("is-horizontal")

    def button: Mod[ReactiveHtmlElement[_]] =
      newMod("button")
    def isInfo: Mod[ReactiveHtmlElement[_]] =
      newMod("is-info")
    def m3: Mod[ReactiveHtmlElement[_]] =
      newMod("m-3")

    def boxed: Mod[ReactiveHtmlElement[_]] =
      newMod("box")

    def p3: Mod[ReactiveHtmlElement[_]] =
      newMod("p3")

    def column: Mod[ReactiveHtmlElement[_]] =
      newMod("column")

    def columns: Mod[ReactiveHtmlElement[_]] =
      newMod("columns")


    def size5: Mod[ReactiveHtmlElement[_]] =
      newMod("is-size-5")

    def hasTextGrey: Mod[ReactiveHtmlElement[_]] =
      newMod("has-text-grey-dark")

    def field: Mod[ReactiveHtmlElement[_]] =
      newMod("field")
    def fieldLabel: Mod[ReactiveHtmlElement[_]] =
      newMod("field-label")
    def fieldBody: Mod[ReactiveHtmlElement[_]] =
      newMod("field-body")

    def icon: Mod[ReactiveHtmlElement[_]] =
      newMod("icon")

    def control: Mod[ReactiveHtmlElement[_]] =
      newMod("control")
    def labelB: Mod[ReactiveHtmlElement[_]] =
      newMod("label")
    def inputB: Mod[ReactiveHtmlElement[_]] =
      newMod("input")

    def fa: Mod[ReactiveHtmlElement[_]] =
      newMod("fa")
    def faAngleDown: Mod[ReactiveHtmlElement[_]] =
      newMod("fa-angle-down")

    private def newMod(clsValue: String): Mod[ReactiveHtmlElement[_]] =
      new Modifier[ReactiveHtmlElement[_]] {
        override def apply(element: ReactiveHtmlElement[_]): Unit = element
          .amend(el)
          .amend(cls := clsValue)
      }
