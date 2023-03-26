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
// build tag 230325-115444
// build tag 230325-120138
// build tag 230325-120311
// build tag 230325-120349
// build tag 230325-120552
// build tag 230325-120917
// build tag 230325-200229
// build tag 230325-215224
// build tag 230325-230035
// build tag 230326-082602
// build tag 230326-083905
// build tag 230326-084624
// build tag 230326-091959
// build tag 230326-094848
// build tag 230326-095858
// build tag 230326-095953
// build tag 230326-100405
// build tag 230326-101234
// build tag 230326-105711
// build tag 230326-110147
// build tag 230326-110653
// build tag 230326-110905
// build tag 230326-112410
// build tag 230326-204738
// build tag 230326-204856
// build tag 230326-213704
// build tag 230326-220718
// build tag 230326-222937
// build tag 230326-231031
// build tag 230326-235819
// build tag 230327-001057
// build tag 230327-001208
// build tag 230327-001831
// build tag 230327-002300
// build tag 230327-003335
