package org.cloudfoundry.loggregator.logmon.anomalies

import org.cloudfoundry.loggregator.logmon.statistics.LogTestExecutionsRepo
import org.cloudfoundry.loggregator.logmon.statistics.StatisticsPresenter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
open class AnomalyStateMachine @Autowired constructor(
    private val anomalyRepo: AnomalyRepo,
    private val logTestExecutionsRepo: LogTestExecutionsRepo,
    private val statistics: StatisticsPresenter
) {
    @PostConstruct
    protected open fun initialize() {
        anomalyRepo.save("Deploy successful, collecting data", AnomalyLevel.GREEN)
    }

    @Value("\${logmon.anomalies.sample-size}")
    private var WINDOW_WIDTH = 5

    private var state: AnomalyLevel = AnomalyLevel.GREEN

    open fun recalculate() {
        val lastN = logTestExecutionsRepo.findAll().takeLast(WINDOW_WIDTH)
        if (lastN.size < WINDOW_WIDTH) return
        val reliability = statistics.reliability(lastN)
        if ((0.9..0.99).contains(reliability) && state != AnomalyLevel.YELLOW) {
            state = AnomalyLevel.YELLOW
            anomalyRepo.save(state.message(reliability * 100), state)
        } else if (reliability < 0.9 && state != AnomalyLevel.RED) {
            state = AnomalyLevel.RED
            anomalyRepo.save(state.message(reliability * 100), state)
        } else if (reliability > 0.99 && state != AnomalyLevel.GREEN) {
            state = AnomalyLevel.GREEN
            anomalyRepo.save(state.message(reliability * 100), state)
        }
    }
}
