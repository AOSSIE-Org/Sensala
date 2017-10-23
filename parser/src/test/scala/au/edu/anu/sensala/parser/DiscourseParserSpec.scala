package au.edu.anu.sensala.parser

import au.edu.anu.sensala.SensalaSpec
import au.edu.anu.sensala.structure._

class DiscourseParserSpec extends SensalaSpec {
  it should "parse simple sentences" in {
    DiscourseParser.parse("John walks") shouldBe Discourse(List(
      Sentence(ProperNoun("John"), IntransitiveVerb("walks"))
    ))
    DiscourseParser.parse("Mary loves herself") shouldBe Discourse(List(
      Sentence(ProperNoun("Mary"), VerbObjPhrase(TransitiveVerb("loves"), ReflexivePronoun("herself")))
    ))
  }

  it should "parse quantified common nouns" in {
    DiscourseParser.parse("A donkey walks") shouldBe Discourse(List(
      Sentence(ExistentialQuantifier(CommonNoun("donkey")), IntransitiveVerb("walks"))
    ))
    DiscourseParser.parse("Every farmer walks") shouldBe Discourse(List(
      Sentence(ForallQuantifier(CommonNoun("farmer")), IntransitiveVerb("walks"))
    ))
    DiscourseParser.parse("Every farmer owns a donkey") shouldBe Discourse(List(
      Sentence(ForallQuantifier(CommonNoun("farmer")), VerbObjPhrase(TransitiveVerb("owns"), ExistentialQuantifier(CommonNoun("donkey"))))
    ))
  }

  it should "parse wh noun phrases" in {
    DiscourseParser.parse("Every farmer who owns a donkey beats it") shouldBe Discourse(List(
      Sentence(
        ForallQuantifier(WhNounPhrase(VerbObjPhrase(TransitiveVerb("owns"), ExistentialQuantifier(CommonNoun("donkey"))), CommonNoun("farmer"))),
        VerbObjPhrase(TransitiveVerb("beats"), ReflexivePronoun("it"))
      )
    ))
  }

  it should "parse multi-sentence discourses" in {
    DiscourseParser.parse("Every farmer who owns a donkey beats it. John is a farmer. John owns a donkey.") shouldBe Discourse(List(
      Sentence(
        ForallQuantifier(WhNounPhrase(VerbObjPhrase(TransitiveVerb("owns"), ExistentialQuantifier(CommonNoun("donkey"))), CommonNoun("farmer"))),
        VerbObjPhrase(TransitiveVerb("beats"), ReflexivePronoun("it"))
      ),
      Sentence(
        ProperNoun("John"),
        VerbObjPhrase(TransitiveVerb("is"), ExistentialQuantifier(CommonNoun("farmer")))
      ),
      Sentence(
        ProperNoun("John"),
        VerbObjPhrase(TransitiveVerb("owns"), ExistentialQuantifier(CommonNoun("donkey")))
      )
    ))
  }
}
