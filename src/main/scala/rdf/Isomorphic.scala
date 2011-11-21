package org.w3.isomorphic

trait PatternMatching0[-R] {
  def unapply(r:R):Boolean
}

//trait Isomorphic0[R] extends Function0[R] with PatternMatching0[R]

trait PatternMatching1[+T,-R] {
  def unapply(r:R):Option[T]
}

trait Isomorphic1[T,R] extends Function1[T,R] with PatternMatching1[T,R]

trait PatternMatching2[+T1,+T2,-R] {
  def unapply(r:R):Option[(T1, T2)]
}

/**
 * basically, you have to implement both following functions
 *   def apply(t1:T1, t2:T2):R
 *   def unapply(r:R):Option[(T1, T2)]
 */
trait Isomorphic2[T1,T2,R] extends Function2[T1, T2, R] with PatternMatching2[T1,T2,R]

trait PatternMatching3[+T1,+T2,+T3,-R] {
  def unapply(r:R):Option[(T1, T2, T3)]
}

trait Isomorphic3[T1,T2,T3,R] extends Function3[T1, T2, T3, R] with PatternMatching3[T1,T2,T3,R]

