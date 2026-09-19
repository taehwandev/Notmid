package app.thdev.glassnavlab.feature.map

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class TestA {

    fun main() {
        a()
    }

    fun a() {
        var count = 0 // 변수 생성.

        GlobalScope.launch {
            while (true) {
                count++
                if (count >= 10) {
                    break
                }
            }
        } // launch 호출
        // 종료.
    }
}