package pe.edu.pucp.melanomaacral.data.files

// Cálculo de reducción de imágenes, sin Android para poder probarlo con pruebas unitarias.
object ImageScaling {

    // Potencia de 2 más grande que, al reducir, deja el lado mayor todavía >= maxSide.
    // (BitmapFactory.inSampleSize solo admite potencias de 2.)
    fun sampleSize(width: Int, height: Int, maxSide: Int): Int {
        require(maxSide > 0) { "maxSide debe ser positivo" }
        var sample = 1
        var side = maxOf(width, height)
        while (side / 2 >= maxSide) {
            side /= 2
            sample *= 2
        }
        return sample
    }

    // Tamaño final con el lado mayor <= maxSide, conservando la proporción.
    fun fit(width: Int, height: Int, maxSide: Int): Pair<Int, Int> {
        val side = maxOf(width, height)
        if (side <= maxSide) return width to height
        val ratio = maxSide.toFloat() / side
        return maxOf(1, (width * ratio).toInt()) to maxOf(1, (height * ratio).toInt())
    }
}
