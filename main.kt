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
        try {
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
        catch (e: Exception) {
            println("An error occurred during initialization: ${e.message}")
            e.printStackTrace()
            exitProcess(0)
        }
    }
}

object Images {
    val image: BufferedImage = InputImage("image").image
    val watermark: BufferedImage = InputImage("watermark").image
    init {
        if (image.width < watermark.width || image.height < watermark.height) {
            print("The watermark's dimensions are larger.").also { exitProcess(0) }
        }
        if (watermark.transparency == 3) {
            println("Do you want to use the watermark's Alpha channel?")
            Blender.alpha = readln().lowercase() == "yes"
        } else {
            println("Do you want to set a transparency color?")
            Blender.transparency = readln().lowercase() == "yes"
            if (Blender.transparency) Blender.setTransparencyColor()
        }
        Blender.setTransparencyPercentage()
    }
}

object OutputImage {
    private val outputFileName: String
    private val outputFileExtension: String
    val positionMethod: String
    val diffX: Int
    val diffY: Int
    init {
        println("Choose the position method (single, grid):")
        positionMethod = readln().also { if (it != "single" && it != "grid")
            println("The position method input is invalid.").also {  exitProcess(0) } }
        val diffXY: List<Int>
        val (maxDiffX, maxDiffY) = listOf(
            (Images.image.width - Images.watermark.width), (Images.image.height - Images.watermark.height))
        if (positionMethod == "single") {
            println("Input the watermark position ([x 0-$maxDiffX] [y 0-$maxDiffY]):")
            val input = readln()
            if (Regex("-?\\d{1,3} -?\\d{1,3}").matches(input)) {
                diffXY = input.split(" ").map { it.toInt() }
            } else println("The position input is invalid.").also {  exitProcess(0) }
            if (diffXY[0] !in 0..maxDiffX || diffXY[1] !in 0..maxDiffY) {
                println("The position input is out of range.").also {  exitProcess(0) }
            }
        } else diffXY = listOf(0, 0)
        diffX = diffXY[0]
        diffY = diffXY[1]
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
                if (it !in 0..255) print("The transparency color input is invalid.").also {exitProcess(0)} } }
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
        // Create an output image of the same size and type as the original
        val outputImage = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                // Get the color of the current pixel from the base image
                val baseColor = Color(image.getRGB(x, y))
                // Get the corresponding watermark pixel based on placement method
                val watermarkColor = getWatermarkPixel(x, y, watermark)
                // Determine the final blended color based on transparency and blending
                val blendedColor = when {
                    alpha && watermarkColor.alpha == 0 -> baseColor
                    isTransparentPixel(watermarkColor) -> baseColor
                    else -> blendColors(baseColor, watermarkColor)
                }
                // Set the blended color in the output image
                outputImage.setRGB(x, y, blendedColor.rgb)
            }
        }
        return outputImage
    }
    // Retrieves the appropriate watermark pixel based on the configured position method
    fun getWatermarkPixel(x: Int, y: Int, watermark: BufferedImage): Color {
        return when (OutputImage.positionMethod) {
            "grid" -> Color(watermark.getRGB(x % watermark.width, y % watermark.height), true)
            "single" -> {
                if (x in OutputImage.diffX until OutputImage.diffX + watermark.width &&
                    y in OutputImage.diffY until OutputImage.diffY + watermark.height) {
                    Color(watermark.getRGB(x - OutputImage.diffX, y - OutputImage.diffY), true)
                } else {
                    Color(0, 0, 0, 0) // Transparent pixel if outside watermark area
                }
            }
            else -> Color(watermark.getRGB(0, 0))
        }
    }
    // Checks if a color matches the defined transparency color
    fun isTransparentPixel(color: Color): Boolean {
        return transparency && color.red == transparencyColor.red &&
                color.green == transparencyColor.green &&
                color.blue == transparencyColor.blue
    }
    // Blends two colors using the given transparency percentage
    fun blendColors(baseColor: Color, watermarkColor: Color): Color {
        return Color(
            (transparencyPercentage * watermarkColor.red + (100 - transparencyPercentage) * baseColor.red) / 100,
            (transparencyPercentage * watermarkColor.green + (100 - transparencyPercentage) * baseColor.green) / 100,
            (transparencyPercentage * watermarkColor.blue + (100 - transparencyPercentage) * baseColor.blue) / 100
        )
    }
}

fun main() {
    Images
    OutputImage.createOutput(Images.image, Images.watermark)
}

