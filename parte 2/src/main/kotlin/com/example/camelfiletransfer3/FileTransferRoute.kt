package com.example.camelfiletransfer3

import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.file.GenericFile
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File

@Component
class FileTransferRoute : RouteBuilder() {

    private val logger = LoggerFactory.getLogger(FileTransferRoute::class.java)

    override fun configure() {
        val inputDir = File("input")
        if (inputDir.exists() && inputDir.isDirectory) {
            logger.info("📂 Carpeta 'input' encontrada. Procesando archivos...")

            from("file:input?noop=true&include=.*\\.csv&recursive=true&delay=1000&idempotent=true")
                .process { exchange ->
                    val file = exchange.getIn().getBody(GenericFile::class.java)
                    val fileName = file.fileName
                    val fileContent = exchange.getIn().getBody(String::class.java)

                    // Validación de encabezados
                    if (!hasValidHeader(fileContent)) {
                        logger.error("❌ Archivo rechazado: '$fileName' - Moviendo a carpeta 'rejected'")
                        File("output/rejected").mkdirs() // Asegurar que la carpeta existe
                        exchange.getIn().setHeader("CamelFileName", "rejected/$fileName")
                        return@process
                    }

                    // Clasificación por tipo de cliente
                    val clientType = determineClientType(fileName)
                    logger.info("📄 Archivo válido: '$fileName'. Tipo: $clientType")

                    // Preparar ruta de destino
                    val targetPath = "$clientType/$fileName"
                    File("output/$clientType").mkdirs()
                    exchange.getIn().setHeader("CamelFileName", targetPath)
                }
                .choice()
                .`when`(simple("\${header.CamelFileName} startsWith 'rejected/'"))
                .to("file:output/")
                .otherwise()
                .to("file:output/")
                .end()
                .log("✅ Archivo procesado: \${header.CamelFileName}")

        } else {
            logger.error("❌ La carpeta 'input' no existe o no es un directorio.")
        }
    }

    private fun hasValidHeader(fileContent: String): Boolean {
        val firstLine = fileContent.lines().firstOrNull() ?: return false
        val separator = when {
            firstLine.contains(',') -> ","
            firstLine.contains(';') -> ";"
            else -> return false
        }
        val columns = firstLine.split(separator)
        return columns.size >= 2 &&
                !columns[0].trim().matches(Regex("^\\d+$")) &&
                columns.any { it.trim().any(Char::isLetter) }
    }

    private fun determineClientType(fileName: String): String {
        return when {
            fileName.contains("VIP", ignoreCase = true) -> "VIP"
            fileName.contains("regular", ignoreCase = true) -> "regular"
            fileName.contains("nuevo", ignoreCase = true) -> "nuevo_cliente"
            else -> "otros"
        }
    }
}