/*
 *      _______                       _____   _____ _____
 *     |__   __|                     |  __ \ / ____|  __ \
 *        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
 *        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/
 *        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |
 *        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|
 *
 * -------------------------------------------------------------
 *
 * TarsosDSP is developed by Joren Six at IPEM, University Ghent
 *
 * -------------------------------------------------------------
 *
 *  Info: http://0110.be/tag/TarsosDSP
 *  Github: https://github.com/JorenSix/TarsosDSP
 *  Releases: http://0110.be/releases/TarsosDSP/
 *
 *  TarsosDSP includes modified source code by various authors,
 *  for credits and info, see README.
 *
 */
package be.tarsos.dsp.util

import java.io.IOException
import java.net.URL

/**
 * Some utility functions to handle audio resources.
 *
 * @author Joren Six
 */
object AudioResourceUtils {
    /**
     * Returns a more practical audio resource name. E.g. if
     * http://stream.com/stream.pls is given, the PLS-file is parsed and the
     * first audio file is returned. It supports PLS, M3U, AXS and XSPF"
     *
     * @param inputResource The input resource, a file, URL, PLS-file or M3U-file.
     * @return A more practical audio resource name.
     */
    @JvmStatic
    fun sanitizeResource(inputResource: String): String {
        return when {
            inputResource.toLowerCase().endsWith("pls") -> {
                parsePLS(inputResource)
            }
            inputResource.toLowerCase().endsWith("m3u") -> {
                parseM3U(inputResource)
            }
            inputResource.toLowerCase().endsWith("asx") -> {
                parseASX(inputResource)
            }
            inputResource.toLowerCase().endsWith("xspf") -> {
                parseXSPF(inputResource)
            }
            else -> inputResource
        }
    }

    private fun parseXSPF(inputResource: String): String {
        var inputFile = ""
        try {
            val contents = readTextFromUrl(URL(inputResource))
            for (line in contents.split("\n".toRegex()).toTypedArray()) {
                if (line.toLowerCase().contains("href")) {
                    val pattern = "(?i)<location>(.*)</location>.*"
                    inputFile = line.replace(pattern.toRegex(), "$1")
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return inputFile
    }

    private fun parseASX(inputResource: String): String {
        var inputFile = ""
        try {
            val contents = readTextFromUrl(URL(inputResource))
            for (line in contents.split("\n".toRegex()).toTypedArray()) {
                if (line.toLowerCase().contains("href")) {
                    val pattern = "(?i).*href=\"(.*)\".*"
                    inputFile = line.replace(pattern.toRegex(), "$1")
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return inputFile
    }

    /**
     * Parses the PLS file and returns the first file name.
     *
     * @param inputUrl The input PLS file.
     * @return The first file name in the PLS playlist.
     */
    @JvmStatic
    fun parsePLS(inputUrl: String?): String {
        var inputFile = ""
        try {
            val plsContents = readTextFromUrl(URL(inputUrl))
            for (line in plsContents.split("\n".toRegex()).toTypedArray()) {
                if (line.startsWith("File1=")) {
                    inputFile = line.replace("File1=", "").trim { it <= ' ' }
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return inputFile
    }

    /**
     * Parses the M3U file and returns the first file name.
     *
     * @param inputUrl The input M3U file.
     * @return The first file name in the M3U play list.
     */
    @JvmStatic
    fun parseM3U(inputUrl: String?): String {
        var inputFile = ""
        try {
            val plsContents = readTextFromUrl(URL(inputUrl))
            for (line in plsContents.split("\n".toRegex()).toTypedArray()) {
                if (!line.trim { it <= ' ' }.isEmpty() && !line.trim { it <= ' ' }.startsWith("#")) {
                    inputFile = line.trim { it <= ' ' }
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return inputFile
    }

    /**
     * Return the text of the file with the given URL. E.g. if
     * http://test.be/text.txt is given the contents of text.txt is returned.
     *
     * @param url The URL.
     * @return The contents of the file.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun readTextFromUrl(url: URL): String {
        try {
            return url.openStream().bufferedReader().lineSequence().joinToString(separator = "\n")
        } catch (exception: IOException) {
            throw exception
        }
    }
}