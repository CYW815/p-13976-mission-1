package com.domain

import jdk.graal.compiler.util.json.JsonParser
import org.graalvm.collections.EconomicMap
import java.io.File
import java.io.FileReader
import java.nio.file.Files

object WiseSaying {
    private var index = 0
    private var lastOrder = ""
    private var lastId = 0
    private val mapList = mutableMapOf<Int, Map<String, String>>()
    init {
        readAllFile()
        println("== 명언 앱 ==")
    }

    fun appStart() {
        while (true) {
            print("명령) ")
            lastOrder = readOrder()

            val data = lastOrder.split("?")

            when(data[0]) {
                "종료" -> break
                "등록" -> write()
                "목록" -> printList()
                "삭제" -> delete(data.getOrElse(1) {""})
                "수정" -> update(data.getOrElse(1) {""})
                "빌드" -> dataBuild()
                else -> continue
            }
        }
    }

    private fun write(){
        print("명언 : ")
        val wiseSaying = readOrder()
        print("작가 : ")
        val author = readOrder()
        lastId = ++index
        mapList[lastId] = mapOf(
            "author" to author,
            "wiseSaying" to wiseSaying
        )
        generatorJsonFile(mapData = mapList[lastId]!!)
        println("${lastId}번 명언이 등록되었습니다.")
    }

    private fun readOrder(): String {
        lastOrder = readlnOrNull()!!.trim()
        return lastOrder
    }

    private fun printList() {
        println("번호 / 작가 / 명언")
        println("----------------------")
        mapList.entries.sortedByDescending { it.key }.forEach { (index, map) ->
            println("${index} / ${map["author"]} / ${map["wiseSaying"]}")
        }
    }

    private fun delete(subData: String) {
        val data = subData.split("=")
        val key = data.getOrElse(0) {""}
        val value = data.getOrElse(1) {""}

        if (key != "id") {
            println("명령 구문을 알맞게 써주세요.")
            return
        }

        value.toIntOrNull()?.let {
            if (mapList[it] == null) {
                println("${it}번 명언은 존재하지 않습니다.")
                return
            }
            mapList.remove(it)
            deleteFile(id = it)
            println("${it}번 명언이 삭제되었습니다.")
        }  ?: println("id값은 숫자만 넣어주세요.")
    }

    private fun update(subData: String) {
        val data = subData.split("=")
        val key = data.getOrElse(0) {""}
        val value = data.getOrElse(1) {""}

        if (key != "id") {
            println("명령 구문을 알맞게 써주세요.")
            return
        }

        value.toIntOrNull()?.let {
            if (mapList[it] == null) {
                println("${it}번 명언은 존재하지 않습니다.")
                return
            }

            println("명언 (기존) : ${mapList[it]?.get("wiseSaying")}")
            print("명언 : ")
            val wiseSaying = readOrder()
            println("명언 (기존) : ${mapList[it]?.get("author")}")
            print("작가 : ")
            val author = readOrder()

            mapList[it] = mapOf(
                "author" to author,
                "wiseSaying" to wiseSaying
            )

            generatorJsonFile(mapData = mapList[it]!!, id = it)
            println("${it}번 명언이 수정되었습니다.")
        }  ?: println("id값은 숫자만 넣어주세요.")
    }

    private fun generatorJsonFile(directoryPath: String = "db/wiseSaying", mapData : Map<String, String>, id: Int = lastId) {
        val jsonString = """
            {
                "id": ${lastId},
                "author": "${mapData["author"]}",
                "wiseSaying": "${mapData["wiseSaying"]}"
            }
        """.trimIndent()

        val file = File(directoryPath, "$id.json")
        if (file.parentFile.exists().not()) {
            file.parentFile.mkdirs()
        }
        file.writeText(jsonString)

        generatorLastIdFile()
    }

    private fun generatorLastIdFile(directoryPath: String = "db/wiseSaying") {
        val file = File(directoryPath, "lastId.txt")
        if (file.parentFile.exists().not()) {
            file.parentFile.mkdirs()
        }
        file.writeText(lastId.toString())
    }

    private fun deleteFile(directoryPath: String = "db/wiseSaying", id: Int) {
        val file = File(directoryPath, "$id.json")
        file.setWritable(true)
        if (!file.exists()) {
            println("파일이 존재하지 않습니다: ${file.absolutePath}")
            return
        }
        Files.delete(file.toPath())
    }

    private fun readAllFile(directoryPath: String = "db/wiseSaying") {
        val file = File(directoryPath)
        file.listFiles()?.filter { it.name.endsWith(".json") }?.forEach {
            FileReader(it).use { reader ->  // use를 사용해서 자동으로 닫기
                val parser = JsonParser(reader)
                val outer = parser.parse() as EconomicMap<String, Any>
                mapList[outer.get("id") as Int] = mapOf(
                    "author" to outer.get("author") as String,
                    "wiseSaying" to outer.get("wiseSaying") as String
                )
            }

        }

        file.listFiles()?.filter { it.name.endsWith(".txt") }?.forEach {
            lastId = it.readText().toInt()
            index = lastId
        }
    }

    private fun dataBuild(directoryPath: String = "data") {
        val file = File(directoryPath, "data.json")
        if (!file.exists() || !file.isDirectory) {
            file.parentFile.mkdirs()
        }

        val jsonString = buildString {
            appendLine("[")

            mapList.entries.forEachIndexed { index, (id, map) ->
                // 들여쓰기 2칸
                append("  {\n")
                append("    \"id\": $id,\n")
                append("    \"wiseSaying\": \"${map["wiseSaying"]}\",\n")
                append("    \"author\": \"${map["author"]}\"\n")
                append("  }")

                // 마지막이 아니면 쉼표와 줄바꿈
                if (index < mapList.size - 1) {
                    appendLine(",")
                } else {
                    appendLine() // 마지막 항목 후 줄바꿈
                }
            }

            append("]")
        }

       file.writeText(jsonString)

    }
}