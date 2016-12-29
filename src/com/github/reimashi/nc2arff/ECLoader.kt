package com.github.reimashi.nc2arff;

import org.kohsuke.args4j.CmdLineException
import org.kohsuke.args4j.CmdLineParser
import org.kohsuke.args4j.Option

import ucar.ma2.ArrayDouble
import ucar.ma2.ArrayFloat
import ucar.ma2.ArrayInt
import ucar.nc2.NetcdfFile
import ucar.nc2.Variable
import java.io.*
import java.text.DecimalFormat
import java.util.*
import java.util.logging.Logger
import java.util.HashSet

class ECLoaderMain {
    val log = Logger.getLogger("Main")

    @Option(name = "-h", usage = "Muestra la ayuda")
    private var showHelp: Boolean = false

    @Option(name = "-i", usage = "Fichero de entrada, en formato NetCDF")
    private var inputFilename: String? = "wrf.nc4"

    @Option(name = "-o", usage = "Fichero de salida, en formato ARFF")
    private var outputFilename: String? = "wrf.arff"

    fun main(args: Array<String>) {
        val parser = CmdLineParser(this)

        try {
            parser.parseArgument(HashSet(args.asList()))

            if (this.showHelp) {
                parser.printUsage(System.out);
            }
            else {
                processDataset(inputFilename!!, outputFilename!!)
            }
        } catch(e: CmdLineException) {
            log.severe("No se han podido parsear los parametros. " + e.message)
        }
    }

    /**
     * Procesa un fichero de entrada en formato NetCDF y lo escribe en un fichero de salida en formato ARFF
     */
    private fun processDataset(wrfPath: String, wrfOut: String) {
        var file: NetcdfFile? = null
        var outFile: File
        var writer: Writer

        try {
            // Se abren los streams de los ficheros de entrada/salida
            file = NetcdfFile.openInMemory(wrfPath)
            outFile = File(wrfOut)
            writer = BufferedWriter(FileWriter(outFile))

            // Se escribe la cabecera del archivo arff
            writer.write("@relation wrf" + System.lineSeparator() + System.lineSeparator())
            writer.write("@attribute time numeric" + System.lineSeparator())
            writer.write("@attribute rain_prec numeric" + System.lineSeparator())
            writer.write("@attribute snow_prec numeric" + System.lineSeparator())
            writer.write("@attribute terrain_height numeric" + System.lineSeparator())
            writer.write("@attribute wind_gust numeric" + System.lineSeparator())
            writer.write("@attribute snow_level numeric" + System.lineSeparator())
            writer.write("@attribute temperature_850mb numeric" + System.lineSeparator())
            writer.write("@attribute temperature_500mb numeric" + System.lineSeparator())
            writer.write("@attribute temperature numeric" + System.lineSeparator())
            writer.write("@attribute relative_humidity numeric" + System.lineSeparator())
            writer.write("@attribute cloud_cover_low numeric" + System.lineSeparator())
            writer.write("@attribute cloud_area_low numeric" + System.lineSeparator())
            writer.write("@attribute cloud_area_mid numeric" + System.lineSeparator())
            writer.write("@attribute cloud_area_high numeric" + System.lineSeparator())
            writer.write("@attribute hot_air_flow_sensible numeric" + System.lineSeparator())
            writer.write("@attribute hot_air_flow_latent numeric" + System.lineSeparator())
            writer.write("@attribute fog {Yes,No}" + System.lineSeparator())
            writer.write(System.lineSeparator() + "@data" + System.lineSeparator())

            // Se cargan las variables dimensionales
            val xDim: Variable? = file.findVariable("x")
            val yDim: Variable? = file.findVariable("y")
            val timeDim: Variable? = file.findVariable("time")

            // Se cargan el resto de variables
            val precVar: Variable? = file.findVariable("prec")
            val snowprecVar: Variable? = file.findVariable("snow_prec")
            val topoVar: Variable? = file.findVariable("topo")
            val windgustVar: Variable? = file.findVariable("wind_gust")
            val snowlevelVar: Variable? = file.findVariable("snowlevel")
            val t500Var: Variable? = file.findVariable("T500")
            val t850Var: Variable? = file.findVariable("T850")
            val tempVar: Variable? = file.findVariable("temp")
            val rhVar: Variable? = file.findVariable("rh")
            val cftVar: Variable? = file.findVariable("cft")
            val cfhVar: Variable? = file.findVariable("cfh")
            val cfmVar: Variable? = file.findVariable("cfm")
            val cflVar: Variable? = file.findVariable("cfl")
            val shflxVar: Variable? = file.findVariable("shflx")
            val lhflxVar: Variable? = file.findVariable("lhflx")
            val visibilityVar: Variable? = file.findVariable("visibility")

            // Si el archivo contiene todas las variables y las ha cargado correctamente
            if (xDim != null && yDim != null && timeDim != null &&
                    topoVar != null && tempVar != null && t500Var != null && t850Var != null &&
                    cfhVar != null && cfmVar != null && cflVar != null && visibilityVar != null && snowlevelVar != null &&
                    snowprecVar != null && precVar != null && rhVar != null && cftVar != null && windgustVar != null && shflxVar != null && lhflxVar != null) {
                // Se obtienen los iteradores de las variables dimensionales
                var xArray = xDim.read() as ArrayDouble.D1
                var yArray = yDim.read() as ArrayDouble.D1
                var timeArray = timeDim.read() as ArrayInt.D1

                // Se obtienen los iteradores del resto de las variables
                var topoArray: ArrayFloat.D3 = topoVar.read(IntArray(4), topoVar.shape).reduce() as ArrayFloat.D3;
                var tempArray: ArrayFloat.D3 = tempVar.read(IntArray(4), tempVar.shape).reduce() as ArrayFloat.D3;
                var t500Array: ArrayFloat.D3 = t500Var.read(IntArray(4), t500Var.shape).reduce() as ArrayFloat.D3;
                var t850Array: ArrayFloat.D3 = t850Var.read(IntArray(4), t850Var.shape).reduce() as ArrayFloat.D3;
                var cftArray: ArrayFloat.D3 = cftVar.read(IntArray(4), cftVar.shape).reduce() as ArrayFloat.D3;
                var cfhArray: ArrayFloat.D3 = cfhVar.read(IntArray(4), cfhVar.shape).reduce() as ArrayFloat.D3;
                var cfmArray: ArrayFloat.D3 = cfmVar.read(IntArray(4), cfmVar.shape).reduce() as ArrayFloat.D3;
                var cflArray: ArrayFloat.D3 = cflVar.read(IntArray(4), cflVar.shape).reduce() as ArrayFloat.D3;
                var visibilityArray: ArrayFloat.D3 = visibilityVar.read(IntArray(4), visibilityVar.shape).reduce() as ArrayFloat.D3;
                var snowlevelArray: ArrayFloat.D3 = snowlevelVar.read(IntArray(4), snowlevelVar.shape).reduce() as ArrayFloat.D3;
                var snowprecArray: ArrayFloat.D3 = snowprecVar.read(IntArray(4), snowprecVar.shape).reduce() as ArrayFloat.D3;
                var precArray: ArrayFloat.D3 = precVar.read(IntArray(4), precVar.shape).reduce() as ArrayFloat.D3;
                var rhArray: ArrayFloat.D3 = rhVar.read(IntArray(4), rhVar.shape).reduce() as ArrayFloat.D3;
                var shflxArray: ArrayFloat.D3 = shflxVar.read(IntArray(4), shflxVar.shape).reduce() as ArrayFloat.D3;
                var lhflxArray: ArrayFloat.D3 = lhflxVar.read(IntArray(4), lhflxVar.shape).reduce() as ArrayFloat.D3;
                var windgustArray: ArrayFloat.D3 = windgustVar.read(IntArray(4), windgustVar.shape).reduce() as ArrayFloat.D3;

                // Se recorren todas las dimensiones
                for (xIndex in 0..(xArray.size.toInt() - 1)) {
                    for (yIndex in 0..(yArray.size.toInt() - 1)) {
                        for (timeIndex in 0..(timeArray.size.toInt() - 1)) {
                            var values = StringJoiner(",")

                            val df = DecimalFormat("#.#####")

                            // Se añade a una linea en formato csv los valores de las variables filtrados
                            values.add(timeArray.get(timeIndex).toString())
                            values.add(df.format(precArray.get(timeIndex, xIndex, yIndex).toDouble()).replace(',', '.'))
                            values.add(df.format(snowprecArray.get(timeIndex, xIndex, yIndex).toDouble()).replace(',', '.'))
                            values.add(df.format(topoArray.get(timeIndex, xIndex, yIndex).toDouble()).replace(',', '.'))
                            values.add(df.format(windgustArray.get(timeIndex, xIndex, yIndex).toDouble()).replace(',', '.'))
                            values.add(df.format(snowlevelArray.get(timeIndex, xIndex, yIndex).toDouble()).replace(',', '.'))
                            values.add(df.format(t850Array.get(timeIndex, xIndex, yIndex).toDouble()).replace(',', '.'))
                            values.add(df.format(t500Array.get(timeIndex, xIndex, yIndex).toDouble()).replace(',', '.'))
                            values.add(df.format(tempArray.get(timeIndex, xIndex, yIndex).toDouble()).replace(',', '.'))
                            values.add(df.format(rhArray.get(timeIndex, xIndex, yIndex).toDouble()).replace(',', '.'))
                            values.add(df.format(cftArray.get(timeIndex, xIndex, yIndex).toDouble()).replace(',', '.'))
                            values.add(df.format(cflArray.get(timeIndex, xIndex, yIndex).toDouble()).replace(',', '.'))
                            values.add(df.format(cfmArray.get(timeIndex, xIndex, yIndex).toDouble()).replace(',', '.'))
                            values.add(df.format(cfhArray.get(timeIndex, xIndex, yIndex).toDouble()).replace(',', '.'))
                            values.add(df.format(shflxArray.get(timeIndex, xIndex, yIndex).toDouble()).replace(',', '.'))
                            values.add(df.format(lhflxArray.get(timeIndex, xIndex, yIndex).toDouble()).replace(',', '.'))

                            // Se obtiene el valor de clase (niebla) de la variable visibilidad
                            var visibilityValue = visibilityArray.get(timeIndex, xIndex, yIndex).toDouble()
                            if (visibilityValue > 5000) values.add("No") else values.add("Yes")

                            // Se escribe la linea de datos al archivo
                            writer.write(values.toString() + System.lineSeparator())
                        }
                    }
                }

                log.info("El archivo WRF se ha convertido a ARFF");
            } else {
                log.severe("El archivo WRF tiene un formato incorrecto y no se ha podido cargar en caché")
                throw IOException("Format incorrect")
            }

            writer.close()
            // Se cierra el stream de salida
        } catch (ioe: IOException) {
            log.severe("Error al abrir un fichero de entrada. " + ioe.message)
        } finally {
            try {
                file?.close()
            } catch (ioe: IOException) {
                log.severe("Error al cerrar los ficheros de entrada. " + ioe.message)
            }
        }
    }
}

fun main(args : Array<String>) {
    var init = ECLoaderMain()
    init.main(args)
}
