package com.sadvakassov.vlur

class Initializer {
    companion object {
        private const val LIBRARY_NAME = "vlur"

        fun instantiate() {
            System.loadLibrary(LIBRARY_NAME)
        }
    }
}
