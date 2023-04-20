package watermark

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import java.awt.Color
import kotlin.system.exitProcess

var imageFile = File("")
var watermarkImageFile = File("")

fun checkAndAssign() {
    println("Input the image filename:")
    imageFile = File(readln())
    if (!imageFile.exists()) println("The file ${imageFile.path} doesn't exist.").also { return }
    val image: BufferedImage = ImageIO.read(imageFile)
    if (!check("image", image)) return // if false -> return
    println("Input the watermark image filename:")
    watermarkImageFile = File(readln())
    if (!watermarkImageFile.exists()) println("The file ${watermarkImageFile.path} doesn't exist.").also { return }
    val watermarkImage: BufferedImage = ImageIO.read(watermarkImageFile)
    if (!check("watermark", watermarkImage)) return
    if (image.width != watermarkImage.width || image.height != watermarkImage.height) println("The image and watermark dimensions are different.").also { return }
    blending(image, watermarkImage)
}

fun blending(image: BufferedImage, watermarkImage: BufferedImage) {
    val alpha = if (watermarkImage.transparency == 3) {
        println("Do you want to use the watermark's Alpha channel?")
        readln().lowercase() == "yes"
    } else false
    val transpColor = if (watermarkImage.transparency != 3) getTranspColor() else listOf()
    val percent = getPercent()
    println("Input the output image filename (jpg or png extension):")
    val outputName = readln()
    if (!(outputName.substring(outputName.length - 4) != ".png").xor(outputName.substring(outputName.length - 4) != ".jpg")) {
        println("The output file extension isn't \"jpg\" or \"png\".")
        return
    }
    val outputFile = File(outputName)
    val output = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
    for (x in 0 until image.width) {
        for (y in 0 until image.height) {
            val i = Color(image.getRGB(x, y), alpha)
            val w = Color(watermarkImage.getRGB(x, y), alpha)
            val color = Color(
                    (percent * w.red + (100 - percent) * i.red) / 100,
                    (percent * w.green + (100 - percent) * i.green) / 100,
                    (percent * w.blue + (100 - percent) * i.blue) / 100
                )
                if (alpha) {
                    if (w.alpha == 0) output.setRGB(x, y, i.rgb)
                    else output.setRGB(x, y, color.rgb)
                } else {
                    output.setRGB(x, y, color.rgb)
            }
            if (transpColor.size == 3) {
                if (w.red == transpColor[0] && w.green == transpColor[1] && w.blue == transpColor[2]) output.setRGB(x, y, i.rgb)
            }
        }
    }
    ImageIO.write(output, outputName.substring(outputName.length - 3), outputFile)
    println("The watermarked image ${outputFile.path} has been created.")
}

fun getPercent(): Int {
    println("Input the watermark transparency percentage (Integer 0-100):")
    val input = readln()
    val percent = if (Regex("\\d{1,3}").matches(input)) {
        input.toInt().also { if (it !in 0..100) {
            println("The transparency percentage is out of range.").also { exitProcess(-1) }
        } }
    } else exitProcess(-1)
    return percent
}

fun transpColorInvalid() = println("The transparency color input is invalid.").also { exitProcess(-1) }

fun getTranspColor(): List<Int> {
    println("Do you want to set a transparency color?")
    var transpColor = listOf<Int>()
    if (readln() == "yes") {
        println("Input a transparency color ([Red] [Green] [Blue]):")
        transpColor = readln().split(' ').map {
            if (it.toIntOrNull() != null && it.toIntOrNull() in 0..255) it.toInt()
            else 0.also { transpColorInvalid() } }
        if (transpColor.size != 3) transpColorInvalid()
    }
    return transpColor
}

fun check(name: String, image: BufferedImage): Boolean {
    val pixelSize = image.colorModel.pixelSize
    val colorComp = image.colorModel.numColorComponents
    if (colorComp != 3) println("The number of $name color components isn't 3.").also { return false }
    if (pixelSize == 24 || pixelSize == 32) return true
    else println("The $name isn't 24 or 32-bit.").also { return false }
}

fun main() {
    checkAndAssign()
//    info(readln())
}
