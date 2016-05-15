/*
 * Copyright 2016 Nicolas Rinaudo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kantan.csv.laws.discipline

import java.io.IOException
import kantan.codecs.laws.discipline.GenCodecValue
import kantan.csv._
import kantan.csv.laws._
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary.{arbitrary => arb}

object arbitrary extends ArbitraryInstances

trait ArbitraryInstances extends kantan.csv.laws.discipline.ArbitraryArities {
  val csv: Gen[List[List[String]]] = arb[List[List[Cell]]].map(_.map(_.map(_.value)))



  // - Errors ----------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  val genOutOfBoundsError: Gen[DecodeError.OutOfBounds] = for(i ← Gen.posNum[Int]) yield DecodeError.OutOfBounds(i)
  val genTypeError: Gen[DecodeError.TypeError] = Gen.const(DecodeError.TypeError(new Exception()))
  val genDecodeError: Gen[DecodeError] = Gen.oneOf(genOutOfBoundsError, genTypeError)

  val genIOError: Gen[ParseError.IOError] = Gen.const(ParseError.IOError(new IOException()))
  val genSyntaxError: Gen[ParseError.SyntaxError] = for {
    i ← arb[Int]
    j ← arb[Int]
  } yield ParseError.SyntaxError(i, j)
  val genParseError: Gen[ParseError] = Gen.oneOf(genIOError, genSyntaxError)

  implicit val arbDecodeError: Arbitrary[DecodeError] = Arbitrary(genDecodeError)
  implicit val arbParseError: Arbitrary[ParseError] = Arbitrary(genParseError)
  implicit val arbReadError: Arbitrary[ReadError] = Arbitrary(Gen.oneOf(genDecodeError, genParseError))



  // - Encoders and decoders -------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  implicit def arbCellDecoder[A: Arbitrary]: Arbitrary[CellDecoder[A]] =
    Arbitrary(arb[String ⇒ DecodeResult[A]].map(f ⇒ CellDecoder(f)))

  implicit def arbCellEncoder[A: Arbitrary]: Arbitrary[CellEncoder[A]] =
    Arbitrary(arb[A ⇒ String].map(f ⇒ CellEncoder(f)))

  implicit def arbRowDecoder[A: Arbitrary]: Arbitrary[RowDecoder[A]] =
    Arbitrary(arb[Seq[String] ⇒ DecodeResult[A]].map(f ⇒ RowDecoder(f)))

  implicit def arbRowEncoder[A: Arbitrary]: Arbitrary[RowEncoder[A]] =
    Arbitrary(arb[A ⇒ Seq[String]].map(f ⇒ RowEncoder(f)))


  // - Codec values ----------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  implicit def rowFromCell[D: Arbitrary](implicit cd: GenCodecValue[String, D]): GenCodecValue[Seq[String], D] =
    GenCodecValue[Seq[String], D](d ⇒ Seq(cd.encode(d)))(es ⇒ es.length != 1 || cd.isIllegal(es.head))
}