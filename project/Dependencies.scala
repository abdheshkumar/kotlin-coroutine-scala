import sbt.*
import sbt.librarymanagement.DependencyBuilders

object Dependencies {


  object Overrides {
    val kotlinCoroutines = "org.jetbrains.kotlinx" % "kotlinx-coroutines-bom" % "1.10.1" pomOnly()
    val all = List(kotlinCoroutines)
  }

  object Kotlin {
    val kotlinCoroutines = "org.jetbrains.kotlinx" % "kotlinx-coroutines-core" % "1.10.1"
  }

  object CatsEffect {
    val mUnitTesting = "org.typelevel" %% "munit-cats-effect-3" % "1.0.6" % Test
    val catsEffect = "org.typelevel" %% "cats-effect" % "3.5.7"
    val catsEffectTestingScalatest = "org.typelevel" %% "cats-effect-testing-scalatest" % "1.6.0" % Test
  }

  val all = List(
    CatsEffect.catsEffect,
    CatsEffect.catsEffectTestingScalatest,
    CatsEffect.mUnitTesting,
    Kotlin.kotlinCoroutines
  )

}
