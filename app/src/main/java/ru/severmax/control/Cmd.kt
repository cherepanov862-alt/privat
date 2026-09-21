package ru.severmax.control

import java.util.Locale

/** SMS-команды подогревателя Севермакс (по инструкции, стр. 23-27). */
object Cmd {
    /** Запуск на N минут: K*040 (по умолчанию без времени работает 30 минут). */
    fun start(minutes: Int): String = String.format(Locale.US, "K*%03d", minutes)

    /** Остановка. Ответ: HEATER OFF OK! */
    fun stop(): String = "G"

    /** Запрос статуса. */
    fun status(): String = "C"

    /** Установка температуры нагрева: NFPZ*65 или CGPZ*65 (зависит от прошивки). */
    fun setTemp(prefix: String, temp: Int): String = "$prefix*$temp"

    /** Режим догрева: XHPZ*верх*низ, например XHPZ*90*30. */
    fun boost(up: Int, down: Int): String = "XHPZ*$up*$down"

    /** Привязка телефона: TJSQ*123456*A (A, B, C, D — слоты 1-4). */
    fun bind(code: String, slot: Char): String = "TJSQ*$code*$slot"
}
