package zk

import org.zkoss.zkgrails.*

class Issue139_2Comet extends GrailsComet {

    int i = 0

    static trigger = [startDelay: 800L, every: 100L]

    def beforeExecute = { desktop, page ->
        lblTest2.value = "begin"
    }

    def execute = { desktop, page ->
        i++
        lblTest2.value = "time2 : ${i}"

        if(i == 5) {
            i = 0
            stop()
        }
    }

    def afterExecute = { desktop, page ->
        lblTest2.value = "end"
    }
}
