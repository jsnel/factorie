/* Copyright (C) 2008-2010 University of Massachusetts Amherst,
   Department of Computer Science.
   This file is part of "FACTORIE" (Factor graphs, Imperative, Extensible)
   http://factorie.cs.umass.edu, http://code.google.com/p/factorie/
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. */

package cc.factorie.generative
import cc.factorie._
import scala.reflect.Manifest
import scala.collection.mutable.{HashSet,HashMap}
import scala.util.Random

object DiscreteMixture extends GenerativeFamily3[GeneratedDiscreteVar,Mixture[Proportions],Gate] {
  case class Factor(_1:GeneratedDiscreteVar, _2:Mixture[Proportions], _3:Gate) extends DiscreteGeneratingFactor with MixtureFactor with super.Factor {
    def gate = _3
    def pr(s:StatisticsType) = s._2(s._3.intValue).apply(s._1.intValue)
    def sampledValue(s:StatisticsType): DiscreteValue = s._1.domain.getValue(s._2(s._3.intValue).sampleInt)
    def prChoosing(s:StatisticsType, mixtureIndex:Int): Double = s._2(mixtureIndex).apply(s._1.intValue)
    override def prChoosing(mixtureIndex:Int): Double = _2(mixtureIndex).apply(_1.intValue)
    def sampledValueChoosing(s:StatisticsType, mixtureIndex:Int): ChildType#Value = s._1.domain.getValue(s._2(mixtureIndex).sampleInt)
    def prValue(f:Statistics, intValue:Int): Double = f._2.apply(f._3.intValue).apply(intValue)
    override def updateCollapsedParents(weight:Double): Boolean = {
      _2(_3.intValue) match {
        case p:DenseCountsProportions => { p.increment(_1.intValue, weight)(null); true }
        case _ => false // Just throw Error instead; change API to return Unit also and always throw Error for unsupported cases
      }
    }
  }
  def newFactor(a:GeneratedDiscreteVar, b:Mixture[Proportions], c:Gate) = Factor(a, b, c)
}

abstract class DiscreteMixtureCounts extends Seq[SortedSparseCounts] {
  def discreteDomain: DiscreteDomain
  def mixtureDomain: DiscreteDomain
  // counts(wordIndex).countOfIndex(topicIndex)
  private val counts = Array.fill(discreteDomain.size)(new SortedSparseCounts(mixtureDomain.size))
  def apply(discreteIndex:Int) = counts(discreteIndex)
  def length = discreteDomain.size
  def iterator = counts.iterator
  def countsTotal: Int = {
    val result = counts.map(_.countsTotal).sum
    var sum = counts.map(_.calculatedCountsTotal).sum
    assert(result == sum, "result="+result+" sum="+sum+"\nresults="+counts.map(_.countsTotal).toSeq.take(20)+"\nsum="+counts.map(_.calculatedCountsTotal).toSeq.take(20))
    result
  }
  val mixtureCounts = new Array[Int](mixtureDomain.size)
  @inline final def increment(discrete:Int, mixture:Int, incr:Int) = {
    mixtureCounts(mixture) += incr
    assert(mixtureCounts(mixture) >= 0)
    counts(discrete).incrementCountAtIndex(mixture, incr)
  }
  def incrementFactor(f:DiscreteMixture.Factor): Unit = increment(f._1.intValue, f._3.intValue, 1)
  def incrementFactor(f:PlatedDiscreteMixture.Factor): Unit = {
    val discretes = f._1
    val gates = f._3
    assert (discretes.length == gates.length)
    var i = discretes.length - 1
    while (i >= 0) {
      increment(discretes(i).intValue, gates(i).intValue, 1)
      i -= 1
    }
  }
}