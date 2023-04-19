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

fun transpColorInvalid() = println("The transparency color input is invalid.").also { exitProcess(-1) }

fun blending(image: BufferedImage, watermarkImage: BufferedImage) {
    val alpha = if (watermarkImage.transparency == 3) {
        println("Do you want to use the watermark's Alpha channel?")
        readln().lowercase() == "yes"
    } else false
    println("Do you want to set a transparency color?")
    var transpColor = listOf<Int>()
    if (readln() == "yes") {
        println("Input a transparency color ([Red] [Green] [Blue]):")
        transpColor = readln().split(' ').map {if (it.toIntOrNull() != 0) it.toInt() else 0.also {transpColorInvalid()} }
    }
    if (transpColor.size != 3) transpColorInvalid()
    println("Input the watermark transparency percentage (Integer 0-100):")
    val percent: Int
    try {
        percent = readln().toInt()
    } catch (e: NumberFormatException) {
        println("The transparency percentage isn't an integer number.")
        return
    }
    if (percent !in 0..100) println("The transparency percentage is out of range.").also { return }
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
            var i = Color(image.getRGB(x, y), alpha)
            var w = Color(watermarkImage.getRGB(x, y), alpha)
            if (transpColor.size == 3) if (w.red == transpColor[0] && w.green == transpColor[1] && w.blue == transpColor[2]) output.setRGB(x, y, i.rgb)
            else {
                val color = Color(
                (percent * w.red + (100 - percent) * i.red) / 100,
                (percent * w.green + (100 - percent) * i.green) / 100,
                (percent * w.blue + (100 - percent) * i.blue) / 100
                )
                if (alpha) {
                    if (w.alpha == 0) output.setRGB(x, y, Color(image.getRGB(x, y)).rgb)
                    else output.setRGB(x, y, color.rgb)
                } else {
                    output.setRGB(x, y, color.rgb)
                }
            }
        }
    }
    ImageIO.write(output, outputName.substring(outputName.length - 3), outputFile)
    println("The watermarked image ${outputFile.path} has been created.")
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
