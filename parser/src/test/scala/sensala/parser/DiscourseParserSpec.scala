package sensala.parser

import sensala.SensalaSpec
import sensala.structure._
import sensala.structure.adjective._
import sensala.structure.adverb.{Adverb, VerbAdverbPhrase}
import sensala.structure.noun._
import sensala.structure.prepositional.InPhrase
import sensala.structure.verb._
import sensala.structure.wh._

class DiscourseParserSpec extends SensalaSpec {
  def parse(discourse: String): Either[String, Discourse] = {
    NewDiscourseParser.parse(discourse)
  }
  
  it should "parse simple sentences" in {
    parse("John walks").right.value shouldBe Discourse(List(
      NounPhraseWithVerbPhrase(ExistentialQuantifier(ProperNoun("John")), IntransitiveVerb("walks"))
    ))
    parse("Mary loves herself").right.value shouldBe Discourse(List(
      NounPhraseWithVerbPhrase(ExistentialQuantifier(ProperNoun("Mary")), TransitiveVerb("loves", ReflexivePronoun("herself")))
    ))
  }

  it should "parse quantified common nouns" in {
    parse("A donkey walks").right.value shouldBe Discourse(List(
      NounPhraseWithVerbPhrase(ExistentialQuantifier(CommonNoun("donkey")), IntransitiveVerb("walks"))
    ))
    parse("Every farmer walks").right.value shouldBe Discourse(List(
      NounPhraseWithVerbPhrase(ForallQuantifier(CommonNoun("farmer")), IntransitiveVerb("walks"))
    ))
    parse("Every farmer owns a donkey").right.value shouldBe Discourse(List(
      NounPhraseWithVerbPhrase(ForallQuantifier(CommonNoun("farmer")), TransitiveVerb("owns", ExistentialQuantifier(CommonNoun("donkey"))))
    ))
  }

  it should "parse wh noun phrases" in {
    parse("Every farmer who owns a donkey beats it").right.value shouldBe Discourse(List(
      NounPhraseWithVerbPhrase(
        ForallQuantifier(WhNounPhrase(TransitiveVerb("owns", ExistentialQuantifier(CommonNoun("donkey"))), CommonNoun("farmer"))),
        TransitiveVerb("beats", ReflexivePronoun("it"))
      )
    ))
    parse("A farmer who owns a donkey beats it").right.value shouldBe Discourse(List(
      NounPhraseWithVerbPhrase(
        ExistentialQuantifier(WhNounPhrase(TransitiveVerb("owns", ExistentialQuantifier(CommonNoun("donkey"))), CommonNoun("farmer"))),
        TransitiveVerb("beats", ReflexivePronoun("it"))
      )
    ))
    parse("A farmer who eats walks").right.value shouldBe Discourse(List(
      NounPhraseWithVerbPhrase(
        ExistentialQuantifier(WhNounPhrase(IntransitiveVerb("eats"), CommonNoun("farmer"))),
        IntransitiveVerb("walks")
      )
    ))
  }
  
  it should "parse multi-sentence discourses" in {
    parse("Every farmer who owns a donkey beats it. John is a farmer. John owns a donkey.").right.value shouldBe Discourse(List(
      NounPhraseWithVerbPhrase(
        ForallQuantifier(WhNounPhrase(TransitiveVerb("owns", ExistentialQuantifier(CommonNoun("donkey"))), CommonNoun("farmer"))),
        TransitiveVerb("beats", ReflexivePronoun("it"))
      ),
      NounPhraseWithVerbPhrase(
        ExistentialQuantifier(ProperNoun("John")),
        TransitiveVerb("is", ExistentialQuantifier(CommonNoun("farmer")))
      ),
      NounPhraseWithVerbPhrase(
        ExistentialQuantifier(ProperNoun("John")),
        TransitiveVerb("owns", ExistentialQuantifier(CommonNoun("donkey")))
      )
    ))
  }
  
  it should "parse sentences with adjectives" in {
    parse("Every wealthy farmer owns a donkey").right.value shouldBe Discourse(List(
      NounPhraseWithVerbPhrase(
        ForallQuantifier(AdjectiveNounPhrase(Adjective("wealthy"), CommonNoun("farmer"))),
        TransitiveVerb("owns", ExistentialQuantifier(CommonNoun("donkey")))
      )
    ))

    parse("Every lawyer believes he is smart").right.value shouldBe Discourse(List(
      NounPhraseWithVerbPhrase(
        ForallQuantifier(CommonNoun("lawyer")),
        VerbSentencePhrase(
          "believes",
          NounPhraseWithVerbPhrase(
            ReflexivePronoun("he"),
            VerbAdjectivePhrase("is", Adjective("smart"))
          )
        )
      )
    ))
  }
  
  it should "parse adverb sentences" in {
    parse("John runs quickly").right.value shouldBe Discourse(List(
      NounPhraseWithVerbPhrase(
        ExistentialQuantifier(ProperNoun("John")),
        VerbAdverbPhrase(Adverb("quickly"), IntransitiveVerb("runs"))
      )
    ))
  }
  
  it should "parse propositional sentences" in {
    parse("John left a wallet on a table").right.value shouldBe Discourse(List(
      NounPhraseWithVerbPhrase(
        ExistentialQuantifier(ProperNoun("John")),
        VerbInPhrase(
          InPhrase("on", ExistentialQuantifier(CommonNoun("table"))), 
          TransitiveVerb("left", ExistentialQuantifier(CommonNoun("wallet")))
        )
      )
    ))
  }
}
