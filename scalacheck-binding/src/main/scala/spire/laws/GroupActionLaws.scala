package spire.laws

import spire.algebra._
import spire.implicits._

import org.typelevel.discipline.Laws

import org.scalacheck.{Arbitrary, Prop}
import org.scalacheck.Prop._

object GroupActionLaws {
  def apply[G: Eq: Arbitrary, A: Eq: Arbitrary] = new GroupActionLaws[G, A] {
    val scalarLaws = GroupLaws[G]
    def EquA = Eq[A]
    def ArbA = implicitly[Arbitrary[A]]
  }
}

trait GroupActionLaws[G, A] extends Laws {

  implicit def scalar(implicit G: GroupAction[A, G]): Semigroup[G] = G.scalar

  val scalarLaws: GroupLaws[G]

  import scalarLaws.{ Equ => EqA, Arb => ArA }

  implicit def EquA: Eq[A]
  implicit def ArbA: Arbitrary[A]

  def semigroupAction(implicit G: GroupAction[A, G]) = new ActionProperties(
    name = "groupAction",
    sl = _.semigroup(G.scalar),
    parent = None,

    "left compatibility" → forAll { (g: G, h: G, a: A) =>
      ((g |+| h) |+|> a) === (g |+|> (h |+|> a))
    },
    "right compatibility" → forAll { (a: A, g: G, h: G) =>
      (a <|+| (g |+| h)) === ((a <|+| g) <|+| h)
    }
  )

  def groupAction(implicit G: GroupAction[A, G], G0: Monoid[G]) = new ActionProperties(
    name = "groupAction",
    sl = _.monoid(G0),
    parent = Some(semigroupAction),

    "identity" → forAll { (a: A) =>
      (G0.id |+|> a) === a
    }
  )

  def additiveGroupAction(implicit G: AdditiveGroupAction[A, G], G0: AdditiveMonoid[G]) = new AdditiveProperties(
    base = groupAction(G.additive, G0.additive),
    parent = None
  )

  def multiplicativeGroupAction(implicit G: MultiplicativeGroupAction[A, G], G0: MultiplicativeMonoid[G]) = new MultiplicativeProperties(
    base = groupAction(G.multiplicative, G0.multiplicative),
    parent = None
  )

  class ActionProperties(
    val name: String,
    val sl: scalarLaws.type => scalarLaws.RuleSet,
    val parent: Option[ActionProperties],
    val props: (String, Prop)*
  ) extends RuleSet {
    val bases = Seq("scalar" → sl(scalarLaws))
    val parents = parent.toSeq
  }

  class AdditiveProperties(
    val base: ActionProperties,
    val parent: Option[AdditiveProperties],
    val props: (String, Prop)*
  ) extends RuleSet with HasOneParent {
    val name = base.name
    val bases = Seq("base" → base)
  }

  class MultiplicativeProperties(
    val base: ActionProperties,
    val parent: Option[MultiplicativeProperties],
    val props: (String, Prop)*
  ) extends RuleSet with HasOneParent {
    val name = base.name
    val bases = Seq("base" → base)
  }
}
