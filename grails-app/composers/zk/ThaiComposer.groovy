package zk

import org.zkoss.zkgrails.GrailsComposer;

class ThaiComposer extends GrailsComposer {

    def lblTest

    def afterCompose = { c ->
        c.append {
            ปุ่มกด (ป้าย: "โอเค")
        }
    }

}
