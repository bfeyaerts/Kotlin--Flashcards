package flashcards

import java.io.File
import java.io.FileNotFoundException
import kotlin.math.exp
import kotlin.random.Random

data class FlashCard(var term: String, var definition: String, var errorCount: Int = 0)
val flashCards = mutableListOf<FlashCard>()
fun findByTerm(term: String) = flashCards.find { it.term == term }
fun findByDefinition(definition: String) = flashCards.find { it.definition == definition }

val log = mutableListOf<String>()

enum class Option(val string: String) {
    IMPORT("-import"),
    EXPORT("-export"),
    ;
    companion object {
        fun valueOfOrNull(string: String): Option? {
            values().forEach {
                if (it.string == string.lowercase())
                    return it
            }
            return null
        }
    }
}

enum class Action(val command: String) {
    ADD("add"),
    REMOVE("remove"),
    IMPORT("import"),
    EXPORT("export"),
    ASK("ask"),
    EXIT("exit"),
    LOG("log"),
    HARDEST_CARD("hardest card"),
    RESET_STATS("reset stats");
    companion object {
        fun valueOfOrNull(string: String): Action? {
            values().forEach {
                if (it.command == string.lowercase())
                    return it
            }
            return null
        }
    }
}

fun printLine(string: String) {
    println(string)
    log.add(string)
}

fun readLine(): String {
    val line = readln()
    log.add(line)
    return line
}

fun writeOrAppend(file: File, index: Int, string: String) =
    if (index == 0) {
        file.writeText(string)
    } else {
        file.appendText(string)
    }

fun main(args: Array<String>) {
    var exportFileName: String? = null
    for(i in args.indices step 2) {
        val option = Option.valueOfOrNull(args[i])
        when (option) {
            Option.IMPORT -> {
                import(args[i + 1])
            }
            Option.EXPORT -> {
                exportFileName = args[i + 1]
            }
        }
    }

    while (true) {
        when(prompt("Input the action (${Action.values().joinToString(", ") { it.name.lowercase() }})", Action::valueOfOrNull)) {
            Action.ADD -> {
                val term = prompt("The card:")
                if (findByTerm(term) != null) {
                    printLine("The card \"$term\" already exists.")
                    continue
                }
                val definition = prompt("The definition of the card:")
                if (findByDefinition(definition) != null) {
                    printLine("The definition \"$definition\" already exists.")
                    continue
                }
                flashCards.add(FlashCard(term, definition))
                printLine("The pair (\"$term\":\"$definition\") has been added.")
            }
            Action.REMOVE -> {
                val term = prompt("Which card?")
                val flashCard = findByTerm(term)
                if (flashCard == null) {
                    printLine("Can't remove \"$term\": there is no such card.")
                    continue
                }
                flashCards.remove(flashCard)
                printLine("The card has been removed.")
            }
            Action.IMPORT -> {
                val filename = prompt("File name:")
                import(filename)
            }
            Action.EXPORT -> {
                val filename = prompt("File name:")
                export(filename)
            }
            Action.ASK -> {
                val numberOfCards = prompt("How many times to ask?").toInt()
                repeat(numberOfCards) {
                    val flashCard = flashCards[Random.nextInt(0, flashCards.size)]
                    val answer = prompt("Print the definition of \"${flashCard.term}\":")
                    val flashCardByDefinition = findByDefinition(answer)
                    if (flashCardByDefinition == null) {
                        flashCard.errorCount += 1
                        printLine("Wrong. The right answer is \"${flashCard.definition}\".")
                    } else if (flashCardByDefinition.term == flashCard.term) {
                        printLine("Correct!")
                    } else {
                        flashCard.errorCount += 1
                        printLine("Wrong. The right answer is \"${flashCard.definition}\", but your definition is correct for \"${flashCardByDefinition.term}\".")
                    }
                }
            }
            Action.EXIT -> {
                printLine("Bye bye!")
                if (exportFileName != null)
                    export(exportFileName)
                return
            }
            Action.LOG -> {
                val file = prompt("File name:") { File(it) }
                log.forEachIndexed { index, logLine ->
                    writeOrAppend(file, index, "$logLine\n")
                }
                printLine("The log has been saved.")
            }
            Action.HARDEST_CARD -> {
                val maxErrorCount = if (flashCards.isEmpty()) 0 else flashCards.maxOf { it.errorCount }
                if (maxErrorCount == 0) {
                    printLine("There are no cards with errors.")
                } else {
                    val hardestCards = flashCards.filter { it.errorCount == maxErrorCount }
                    if (hardestCards.size == 1) {
                        val hardestCard = hardestCards[0]
                        printLine("The hardest card is \"${hardestCard.term}\". You have $maxErrorCount errors answering it")
                    } else {
                        printLine("The hardest cards are ${hardestCards.joinToString(", ") { "\"${it.term}\"" }}. You have $maxErrorCount errors answering them")
                    }
                }
            }
            Action.RESET_STATS -> {
                flashCards.forEach { it.errorCount = 0 }
                printLine("Card statistics have been reset.")
            }
            else -> printLine("Action not recognized. Please try again.")
        }
        printLine("")
    }
}

fun export(filename: String) {
    val file = File(filename)
    flashCards.forEachIndexed { index, flashCard ->
        writeOrAppend(file, index, "${flashCard.term}\n${flashCard.definition}\n${flashCard.errorCount}\n")
    }
    printLine("${flashCards.size} cards have been saved.")

}

fun import(filename: String) {
    val file = File(filename)
    if (!file.exists()) {
        printLine("File not found.")
        return
    }
    val lines = file.readLines()
    for (i in 0 until lines.size - 1 step 3) {
        val term = lines[i]
        val definition = lines[i+1]
        val errorCount = lines[i+2].toInt()

        val flashCardByTerm = findByTerm(term)
        val flashCard = if (flashCardByTerm != null) {
            flashCardByTerm
        } else {
            val flashCardByDefinition = findByDefinition(definition)
            if (flashCardByDefinition != null) {
                flashCardByDefinition
            } else {
                val newFlashCard = FlashCard(term, definition, errorCount)
                flashCards.add(newFlashCard)
                newFlashCard
            }
        }
        flashCard.term = term
        flashCard.definition = definition
        flashCard.errorCount = errorCount
    }
    printLine("${lines.size / 3} cards have been loaded.")
}

fun prompt(question: String): String {
    printLine(question)
    return readLine()
}
fun <T> prompt(question: String, map: (String) -> T): T {
    printLine(question)
    return map(readLine())
}

fun prompt(question: String, predicate: (String) -> Boolean, retry: (String) -> String): String {
    printLine(question)
    while (true) {
        val response = readLine()
        try {
            require(predicate(response))
            return response
        } catch (e: IllegalArgumentException) {
            printLine(retry(response))
        }
    }
}