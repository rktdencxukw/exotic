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
// build tag 230323-121515
// build tag 230323-131553
// build tag 230323-132325
// build tag 230323-132856
// build tag 230323-133842
// build tag 230323-134002
// build tag 230323-134636
// build tag 230323-172825
// build tag 230323-172902
// build tag 230323-173701
// build tag 230323-174119
// build tag 230323-174713
// build tag 230323-180351
// build tag 230323-181352
// build tag 230323-182036
// build tag 230323-184218
// build tag 230325-093322
// build tag 230325-093526
// build tag 230325-094727
// build tag 230325-094932
// build tag 230325-095940
// build tag 230325-100434
// build tag 230325-100943
// build tag 230325-101217
