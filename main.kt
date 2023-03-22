package watermark

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

var imageFile = File("")
var watermarkImageFile = File("")

fun info(name: String) {
    val imageFile = File(name)
    if (!imageFile.exists()) {
        println("The file ${name} doesn't exist.")
        return
    }
    val image: BufferedImage = ImageIO.read(imageFile)
    println("Image file: ${name}")
    println("Width: ${image.width}")
    println("Height: ${image.height}")
    println("Number of components: ${image.colorModel.numComponents}")
    println("Number of color components: ${image.colorModel.numColorComponents}")
    println("Bits per pixel: ${image.colorModel.pixelSize}")
    when (image.transparency) {
        1 -> println("Transparency: OPAQUE")
        2 -> println("Transparency: BITMASK")
        3 -> println("Transparency: TRANSLUCENT")
    }
}

fun checkComponents (name: String, numComponents: Int): Boolean {
    if (numComponents != 3) {
        println("The number of \"${name}\" color components isn't 3.")
        return true
    } else return false
}

fun checkBits (name: String, numBits: Int): Boolean {
    if (numBits != 24 || numBits != 32) {
        println("The \"${name}\" isn't 24 or 32-bit.")
        return true
    } else return false
}

fun checkAndAssign() {
    println("Input the image filename:")
    imageFile = File(readln())
    if (!imageFile.exists()) {
        println("The file ${imageFile.name} doesn't exist.")
        return
    }
    val image: BufferedImage = ImageIO.read(imageFile)
    if (checkComponents("image", image.colorModel.numColorComponents) || checkBits("image", image.colorModel.pixelSize)) return
    println("Input the watermark image filename:")
    watermarkImageFile = File(readln())
    if (!imageFile.exists()) {
        println("The file ${watermarkImageFile.name} doesn't exist.")
        return
    }
    if (checkComponents("watermark", image.colorModel.numColorComponents) || checkBits("watermark", image.colorModel.pixelSize)) return
}

fun main() {
    checkAndAssign()
//    info(readln())
}
