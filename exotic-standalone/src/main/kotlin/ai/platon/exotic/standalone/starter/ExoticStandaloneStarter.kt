package ai.platon.exotic.standalone.starter

import ai.platon.exotic.driver.common.ExoticUtils

fun main(argv: Array<String>) {
    ExoticUtils.prepareDatabaseOrFail()

    val executor = ExoticExecutor(argv)
    executor.parseCmdLine()
    executor.execute()
}
// build tag 230323-114324
// build tag 230323-114956
