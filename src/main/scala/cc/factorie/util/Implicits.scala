/* Copyright (C) 2008-2009 Univ of Massachusetts Amherst, Computer Science Dept
   This file is part of "FACTORIE" (Factor graphs, Imperative, Extensible)
   http://factorie.cs.umass.edu, http://code.google.com/p/factorie/
   This software is provided under the terms of the Eclipse Public License 1.0
   as published by http://www.opensource.org.  For further information,
   see the file `LICENSE.txt' included with this distribution. */

package cc.factorie.util

import scala.util.Random
import scala.util.Sorting
import scala.reflect.Manifest
import java.io.File
import cc.factorie.util._


// TODO consider moving to cc.factorie.Implicits??

/** Implicit conversions used in FACTORIE, and which might also be useful to FACTORIE users. */
object Implicits {
  
  // TODO Consider using a similar trick to avoid the need for .init in Template with Statistics!!!
  implicit def sampler2GenericSampler[C](s:Sampler[C])(implicit mc:Manifest[C]) = new GenericSampler[C](s)(mc)
  
  
  // http://debasishg.blogspot.com/2009/09/thrush-combinator-in-scala.html
  case class Thrush[A](x: A) {
    def into[B](g: A => B): B = { g(x) }
  }
  implicit def int2Thrush(x:Int) = Thrush(x)
  implicit def double2Thrush(x:Double) = Thrush(x)
  implicit def anyRefThrush[T<:AnyRef](x:T) = Thrush(x)

  implicit def stringExtras(s: String) = new {
    /**Implements Levenshtein Distance, with specific operation costs to go from this to s2.  Original version was from scalanlp. */
    def editDistance(s2: String, substCost: Int, deleteCost: Int, insertCost: Int): Int = {
      if (s.length == 0) s2.length
      else if (s2.length == 0) s.length
      else {
        val d = new Array[Array[Int]](s.length + 1, s2.length + 1)
        for (i <- 0 to s.length)
          d(i)(0) = i * deleteCost
        for (i <- 0 to s2.length)
          d(0)(i) = i * insertCost
        for (i <- 1 to s.length; j <- 1 to s2.length) {
          val cost = if (s(i - 1) == s2(j - 1)) 0 else substCost
          d(i)(j) = Math.min(d(i - 1)(j) + deleteCost, Math.min(d(i)(j - 1) + insertCost, d(i - 1)(j - 1) + cost))
        }
        d(s.length)(s2.length)
      }
    }

    def editDistance(s2: String): Int = editDistance(s2, 1, 1, 1)
  }

 
  implicit def realValue2Double(r:RealValue): Double = r.doubleValue
  // implicit def intValue2Int(r:IntValue): Int = r.intValue // TODO Should I add this also?
  // implicit def booleanValue2Boolean(r:BooleanValue): Boolean = r.booleanValue // TODO Should I add this also?
  
 
  implicit def iterableExtras[T](s: Iterable[T]) = new {
    //println("iterableExtras constructed with s="+s)
    def sum(extractor: T => Double): Double = s.foldLeft(0.0)((sum, x: T) => sum + extractor(x))
    // TODO I would love to change "sumInts" to simply "sum" but the type inferencer seems to have trouble with seq.sum(_ score)
    def sumInts(extractor: T => Int): Int = s.foldLeft(0)((sum, x: T) => sum + extractor(x))

    def product(extractor: T => Double): Double = s.foldLeft(1.0)((prod, x) => prod * extractor(x))

    def productInts(extractor: T => Int): Int = s.foldLeft(1)((prod, x) => prod * extractor(x))

    def max(extractor: T => Double): T = {
      val xs = s.elements
      if (!xs.hasNext) throw new IllegalArgumentException("<empty>.max((x:T)=>Double)")
      var maxElement = xs.next
      var maxValue = extractor(maxElement)
      while (xs.hasNext) {
        val x = xs.next
        val v = extractor(x)
        if (v > maxValue) {
          maxElement = x
          maxValue = v
        }
      }
      maxElement
    }

    def maxInt(extractor: T => Int): T = {
      val xs = s.elements
      if (!xs.hasNext) throw new IllegalArgumentException("<empty>.maxInt((x:T)=>Int)")
      var maxElement = xs.next
      var maxValue = extractor(maxElement)
      while (xs.hasNext) {
        val x = xs.next
        val v = extractor(x)
        if (v > maxValue) {
          maxElement = x
          maxValue = v
        }
      }
      maxElement
    }

    def min(extractor: T => Double): T = {
      val xs = s.elements
      if (!xs.hasNext) throw new IllegalArgumentException("<empty>.max((x:T)=>Double)")
      var minElement = xs.next
      var minValue = extractor(minElement)
      while (xs.hasNext) {
        val x = xs.next
        val v = extractor(x)
        if (v < minValue) {
          minElement = x
          minValue = v
        }
      }
      minElement
    }

    def minInt(extractor: T => Int): T = {
      val xs = s.elements
      if (!xs.hasNext) throw new IllegalArgumentException("<empty>.minInt((x:T)=>Int)")
      var minElement = xs.next
      var minValue = extractor(minElement)
      while (xs.hasNext) {
        val x = xs.next
        val v = extractor(x)
        if (v < minValue) {
          minElement = x
          minValue = v
        }
      }
      minElement
    }

    def sumAndMin(extractor: T => Double): (Double, T) = {
      var sum = 0.0
      val xs = s.elements
      if (!xs.hasNext) throw new IllegalArgumentException("<empty>.max((x:T)=>Double)")
      var minElement = xs.next
      var minValue = extractor(minElement)
      sum += minValue
      while (xs.hasNext) {
        val x = xs.next
        val v = extractor(x)
        sum += v
        if (v < minValue) {
          minElement = x
          minValue = v
        }
      }
      (sum, minElement)
    }

    /**Returns both the maximum element and the second-to-max element */
    // TODO reimplement this to make it more efficient; no need to sort the whole sequence
    def max2(extractor: T => Double): (T, T) = {
      val s1 = s.toSeq
      assert(s1.length > 1)
      val s2: Seq[T] = Sorting.stableSort(s1, (x1: T, x2: T) => extractor(x1) > extractor(x2))
      (s2(0), s2(1))
    }
    //def filterByClass[X](implicit m:Manifest[X]) = s.filter(x:T => m.erasure.isAssignableFrom(x.getClass)).asInstanceOf[Seq[X]]

    /** Sorts with minimum first. */
    def sortForward(extractor: T => Double): Seq[T] =
      Sorting.stableSort(s.toSeq, (x1: T, x2: T) => extractor(x1) < extractor(x2))

    /** Sorts with maximum first.*/
    def sortReverse(extractor: T => Double): Seq[T] =
      Sorting.stableSort(s.toSeq, (x1: T, x2: T) => extractor(x1) > extractor(x2))

    def sample(random: Random): T = {
      val s2 = s.toSeq
      if (s2.size == 1) s2.first
      else s2(random.nextInt(s2.size))
    }
    def sample : T = sample(Global.random)
  
    def sampleFiltered(random: Random, filterTest: T => Boolean): T = {
      val s2 = s.toSeq.filter(filterTest)
      s2(random.nextInt(s2.size));
    }
    def sampleFiltered(filterTest: T => Boolean): T = sampleFiltered(Global.random, filterTest)
    // TODO use defaults for this when 2.8 comes out

    def sampleProportionally(extractor: T => Double): T = sampleProportionally(Global.random, extractor)
    def sampleProportionally(random:Random, extractor: T => Double): T = {
      //println("sampleProportionally called with Iteratible="+s)
      //if (s.size == 1) return s.first
      var sum = s.foldLeft(0.0)((total, x) => total + extractor(x))
      val r = random.nextDouble * sum
      sum = 0
      for (choice <- s) {
        val e = extractor(choice)
        //println("sampleProportionally e = "+e)
        if (e < 0.0) throw new Error("BonusIterable sample extractor value " + e + " less than zero.  Sum=" + sum)
        sum += e
        if (sum >= r)
          return choice;
      }
      throw new Error("BonusIterable sample error: r=" + r + " sum=" + sum)
    }
    def sampleExpProportionally(random:Random, extractor: T => Double): T = {
      val maxValue : Double = s.foldLeft(Math.NEG_INF_DOUBLE)((max,t) => {val x = extractor(t); assert(x==x); if (x>max) x else max})
      if (maxValue == Math.NEG_INF_DOUBLE) throw new Error("Cannot sample from an empty list.")
      sampleProportionally(random, t => if (extractor(t) == Math.NEG_INF_DOUBLE) Math.NEG_INF_DOUBLE else Math.exp(extractor(t) - maxValue))
    }
    def sampleExpProportionally(extractor: T => Double): T = sampleExpProportionally(Global.random, extractor)

    def shuffle : Seq[T] = shuffle(Global.random)
    def shuffle(random: Random) : Seq[T] = {
      val s2 = s.map(x => (x, random.nextInt)).toSeq
      Sorting.stableSort(s2, (t1: (T, int), t2: (T, int)) => t1._2 > t2._2).map(t => t._1)
    }

    def split(ratio: Double): (Seq[T], Seq[T]) = {
      val s2 = s.toSeq
      if (ratio <= 0 || ratio > 1.0) throw new Error
      val index = (ratio * s2.size).toInt
      if (index >= s2.size)
        (s2, Seq.empty)
      else
        (s2.slice(0, index), s2.drop(index))
    }

    def filterByType[S](implicit m: Manifest[S]): Iterable[S] =
      for (x <- s; if (m.erasure.isAssignableFrom(x.asInstanceOf[AnyRef].getClass))) yield x.asInstanceOf[S]

    def filterByClass[C](c: Class[C]): Iterable[C] =
      for (x <- s; if (c.isAssignableFrom(x.asInstanceOf[AnyRef].getClass))) yield x.asInstanceOf[C]
  }

  /*
  implicit def listExtras[A](list:List[A]) = new {
    def reverseForeach(f: A => Unit): Unit = {
      def loop(l: List[A]): Unit = l match {
        case Nil =>
        case head :: tail => { loop(tail); f(head) }
      }
      loop(list)
    }
  }
*/

  implicit def fileExtras(file: File) = new {
    import java.io.{ByteArrayOutputStream, FileInputStream}
    def contentsAsString: String = {
      val bos = new ByteArrayOutputStream
      val ba = new Array[Byte](2048)
      val is = new FileInputStream(file)
      def read {
        is.read(ba) match {
          case n if n < 0 =>
          case 0 => read
          case n => bos.write(ba, 0, n); read
        }
      }
      read
      bos.toString("UTF-8")
    }
  }

  /**Provides extensions to Any. */
  implicit def anyExtras[T <: AnyRef](t: T) = new {
    /**if t is non-null, return it, otherwise, evaluate and return u. */
    def ?:[U >: T](u: => U) = if (t eq null) u else t;

    /**Intern for arbitrary types */
    def intern: T = {
      val in: Interner[T] = Interner.forClass(t.getClass.asInstanceOf[Class[T]])
      in(t)
    }

    /**if t is non-null return Some(t), otherwise None */
    def toOption = if (t eq null) None else Some(t)
  }

  implicit def doubleExtras(d: Double) = new {
    def =~=(e: Double) = d == e || Math.abs(d - e) / d < 1E-4
  }

  implicit def seqExtras[T](s: Seq[T]) = new {
    // useful subset selection
    def apply(x: Seq[Int]) = x.projection.map(s)
  }

  implicit def randomExtras(r: Random) = new {
    def flip(p: Double) = r.nextDouble < p
  }

}

/*
  class BonusSource(s:Source) {
    def asString = s.getLines.foldLeft(new StringBuffer)(_ append _).toString
  }
  implicit def bonusSource(s:Source) = new BonusSource(s)
*/

