package watermark

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import java.awt.Color
import kotlin.system.exitProcess

fun fileCreator(fileName: String): File {
    val separator = File.separator // get file separator, depends on the system
    val workingDirectory = System.getProperty ("user.dir") // get working directory
    val filePath = "${workingDirectory}${separator}" + // form filePath and replace "/" to system separator
            if ("/" in fileName) fileName.replace("/", separator) else fileName
    return File(filePath)
}

class InputImage(type: String) {
    private val fileName: String
    private val imageFile: File
    val image: BufferedImage
    init {
        println("Input the ${if (type == "watermark") "watermark image" else type} filename:")
        fileName = readln()
        imageFile = fileCreator(fileName) // create image File
        if (imageFile.exists()) {
            image = ImageIO.read(imageFile)
            if (image.colorModel.numColorComponents != 3) println("The number of $type color components isn't 3.").also { exitProcess(0) }
            if (image.colorModel.pixelSize != 24 && image.colorModel.pixelSize != 32) println("The $type isn't 24 or 32-bit.").also { exitProcess(0) }
        } else {
            print("The file $fileName doesn't exist.")
            exitProcess(0)
        }
    }
}

object OutputImage {
    private val outputFileName: String
    private val outputFileExtension: String
    init {
        println("Input the output image filename (jpg or png extension):")
        outputFileName = readln()
        outputFileExtension = outputFileName.substringAfter(".")
        if (outputFileExtension != "jpg" && outputFileExtension != "png") {
            print("The output file extension isn't \"jpg\" or \"png\".").also { exitProcess(0) }
        }
    }

    fun createOutput(image: BufferedImage, watermark: BufferedImage) {
        val outputImage = Blender.blendImage(image, watermark)
        val outputFile = fileCreator(outputFileName)
        ImageIO.write(outputImage, outputFileExtension, outputFile)
        print("The watermarked image $outputFileName has been created.")
    }
}

object Blender {
    var alpha = false
    var transparency = false
    private var transparencyColor = Color(0, 0, 0)
    private var transparencyPercentage = 0
    fun setTransparencyColor() {
        println("Input a transparency color ([Red] [Green] [Blue]):")
        val input = readln()
        if (Regex("\\d{1,3} \\d{1,3} \\d{1,3}").matches(input)) {
            val (red, green, blue) = input.split(" ").map { it.toInt().also {
                if (it !in 0..255) print("The transparency color input is invalid.").also {exitProcess(0)}
            } }
            transparencyColor = Color(red, green, blue)
        } else print("The transparency color input is invalid.").also { exitProcess(0) }
    }

    fun setTransparencyPercentage() {
        println("Input the watermark transparency percentage (Integer 0-100):")
        val input = readln()
        if (Regex("\\d{1,3}").matches(input)) {
            transparencyPercentage = input.toInt().also { if (it !in 0..100) {
                print("The transparency percentage is out of range.").also { exitProcess(0) }
            } }
        } else print("The transparency percentage isn't an integer number.").also { exitProcess(0) }
    }

    fun blendImage(image: BufferedImage, watermark: BufferedImage): BufferedImage {
        val outputImage = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)

        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                val i = Color(image.getRGB(x, y))
                val w = Color(watermark.getRGB(x, y), true)
                var color = Color(i.red, i.green, i.blue)
                if (alpha && w.alpha == 0) {
                    outputImage.setRGB(x, y, color.rgb)
                } else if (transparency &&
                    w.red == transparencyColor.red &&
                    w.green == transparencyColor.green &&
                    w.blue == transparencyColor.blue) {
                    outputImage.setRGB(x, y, color.rgb)
                } else {
                    color = Color(
                        (transparencyPercentage * w.red + (100 - transparencyPercentage) * i.red) / 100,
                        (transparencyPercentage * w.green + (100 - transparencyPercentage) * i.green) / 100,
                        (transparencyPercentage * w.blue + (100 - transparencyPercentage) * i.blue) / 100)
                    outputImage.setRGB(x, y, color.rgb)
                }
            }
        }

        return outputImage
    }
}

fun main() {
    val input = InputImage("image")
    val watermark = InputImage("watermark")
    if (input.image.width < watermark.image.width && input.image.height < watermark.image.height) {
        print("The watermark's dimensions are larger").also { exitProcess(0) }
    }
    if (watermark.image.transparency == 3) {
        println("Do you want to use the watermark's Alpha channel?")
        Blender.alpha = readln().lowercase() == "yes"
    } else {
        println("Do you want to set a transparency color?")
        Blender.transparency = readln().lowercase() == "yes"
        if (Blender.transparency) Blender.setTransparencyColor()
    }
    Blender.setTransparencyPercentage()

    val output = OutputImage
    output.createOutput(input.image, watermark.image)
}
