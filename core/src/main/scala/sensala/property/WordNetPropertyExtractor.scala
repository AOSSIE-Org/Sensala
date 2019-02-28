package sensala.property

import cats.effect.Sync
import cats.implicits._
import net.didion.jwnl.JWNL
import net.didion.jwnl.data._
import net.didion.jwnl.data.list._
import net.didion.jwnl.dictionary.Dictionary
import org.aossie.scavenger.expression.Sym
import sensala.shared.effect.Log
import sensala.structure._

import scala.collection.convert.ImplicitConversionsToScala._

final case class WordNetPropertyExtractor[F[_]: Sync: Log] private (
  private val dictionary: Dictionary,
  private val pointerUtils: PointerUtils
) {
  private def getTargets(synset: Synset, typ: PointerType): Array[PointerTarget] =
    synset.getPointers.filter(_.getType == typ).map(_.getTarget)

  private def makePointerTargetTreeList(
    synset: Synset,
    searchTypes: Array[PointerType],
    labelType: PointerType,
    depth: Int,
    allowRedundancies: Boolean,
    parent: PointerTargetTreeNode
  ): PointerTargetTreeNodeList = {
    val list = new PointerTargetTreeNodeList
    for (typ <- searchTypes) {
      val targets = new PointerTargetNodeList(getTargets(synset, typ))
      if (targets.size > 0) {
        for (itr <- targets.toList.map(_.asInstanceOf[PointerTargetNode])) {
          val node = new PointerTargetTreeNode(
            itr.getPointerTarget,
            if (labelType == null) typ else labelType,
            parent
          )
          if (allowRedundancies || !list.contains(node)) {
            if (depth != 1) {
              node.setChildTreeList(
                makePointerTargetTreeList(
                  node.getSynset,
                  searchTypes,
                  labelType,
                  depth - 1,
                  allowRedundancies,
                  node
                )
              )
            }
            list.add(node)
          }
        }
      }
    }
    list
  }

  private def makePointerTargetTreeList(
    set: Synset,
    searchType: PointerType,
    depth: Int
  ): F[PointerTargetTreeNodeList] =
    Sync[F].delay {
      makePointerTargetTreeList(
        set,
        Array[PointerType](searchType),
        null,
        depth,
        allowRedundancies = true,
        null
      )
    }

  private def getHypernymTree(synset: Synset): F[PointerTargetTree] =
    makePointerTargetTreeList(synset, PointerType.HYPERNYM, Integer.MAX_VALUE) >>= { treeList =>
      new PointerTargetTree(
        synset,
        treeList
      ).pure[F]
    }

  def extractProperties(word: String): F[List[Property]] =
    dictionary.lookupAllIndexWords(word).getIndexWordArray.toList.flatTraverse[F, Property] {
      indexWord =>
        indexWord.getSenses.toList.flatTraverse[F, Property] { sense =>
          for {
            _          <- Log[F].debug(s"Trying sense for $word: ${sense.getGloss}")
            treeEither <- getHypernymTree(sense).attempt
            tree       = treeEither.map(tree => tree.toList.toList)
            result <- tree.getOrElse(List.empty).flatTraverse[F, Property] { pathAny =>
                       val path = pathAny.asInstanceOf[PointerTargetNodeList]
                       path.toList.flatTraverse[F, Property] { nodeAny =>
                         val node       = nodeAny.asInstanceOf[PointerTargetNode]
                         val properties = node.getSynset.getWords.map(_.getLemma).toList
                         Log[F].debug(properties.mkString(", ")) >>
                           properties.map(lemma => Property(x => Sym(lemma)(x))).pure[F]
                       }
                     }
          } yield result
        }
    }
}

object WordNetPropertyExtractor {
  def apply[F[_]](implicit ev: WordNetPropertyExtractor[F]): WordNetPropertyExtractor[F] = ev

  def create[F[_]: Sync: Log](): F[WordNetPropertyExtractor[F]] =
    for {
      _            <- Sync[F].delay { JWNL.initialize(getClass.getResourceAsStream("/jwnl-properties.xml")) }
      dictonary    = Dictionary.getInstance()
      pointerUtils = PointerUtils.getInstance()
    } yield WordNetPropertyExtractor(dictonary, pointerUtils)
}
