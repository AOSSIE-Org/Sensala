package sensala.parser

import sensala.SensalaSpec
import sensala.structure._
import sensala.structure.adjective._
import sensala.structure.adverb._
import sensala.structure.noun._
import sensala.structure.prepositional.InPhrase
import sensala.structure.verb._
import sensala.structure.wh._

class ACL2018SensalaDemoSpec extends SensalaSpec {
  def parse(discourse: String): Either[String, Discourse] = {
    NewDiscourseParser.parse(discourse)
  }
  
  it should "parse example from ACL 2018 system demonstration paper" in {
    parse("John loves Mary").right.value shouldBe Discourse(List(
      NounPhraseWithVerbPhrase(ExistentialQuantifier(ProperNoun("John")), TransitiveVerb("loves", ExistentialQuantifier(ProperNoun("Mary"))))
    ))
    
    parse("John runs quickly").right.value shouldBe Discourse(List(
      NounPhraseWithVerbPhrase(ExistentialQuantifier(ProperNoun("John")), VerbAdverbPhrase(Adverb("quickly"), IntransitiveVerb("runs")))
    ))
    
    parse("John loves Mary. She believes him.").right.value shouldBe Discourse(List(
      NounPhraseWithVerbPhrase(ExistentialQuantifier(ProperNoun("John")), TransitiveVerb("loves", ExistentialQuantifier(ProperNoun("Mary")))),
      NounPhraseWithVerbPhrase(ReflexivePronoun("She"), TransitiveVerb("believes", ReflexivePronoun("him")))
    ))

    parse("John ate a pizza with a fork. Bob did too.").right.value shouldBe Discourse(List(
      NounPhraseWithVerbPhrase(
        ExistentialQuantifier(ProperNoun("John")),
        VerbInPhrase(
          InPhrase("with", ExistentialQuantifier(CommonNoun("fork"))),
          TransitiveVerb("ate", ExistentialQuantifier(CommonNoun("pizza")))
        )
      ),
      NounPhraseWithVerbPhrase(
        ExistentialQuantifier(ProperNoun("Bob")),
        VerbPhraseAnaphora("did too")
      )
    ))

    parse("Mary is loved by John. So is Ann.").right.value shouldBe Discourse(List(
      NounPhraseWithVerbPhrase(ExistentialQuantifier(ProperNoun("John")), TransitiveVerb("loved", ExistentialQuantifier(ProperNoun("Mary")))),
      NounPhraseWithVerbPhrase(
        ExistentialQuantifier(ProperNoun("Ann")),
        VerbPhraseAnaphora("So is")
      )
    ))

    parse("Every farmer who owns a donkey thinks he is rich").right.value shouldBe Discourse(List(
      NounPhraseWithVerbPhrase(
        ForallQuantifier(
          WhNounPhrase(
            TransitiveVerb("owns", ExistentialQuantifier(CommonNoun("donkey"))),
            CommonNoun("farmer")
          )
        ),
        VerbSentencePhrase("thinks", NounPhraseWithVerbPhrase(ReflexivePronoun("he"), VerbAdjectivePhrase("is", Adjective("rich"))))
      )
    ))
  }
}
