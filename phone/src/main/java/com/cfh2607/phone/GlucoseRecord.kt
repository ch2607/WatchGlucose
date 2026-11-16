package com.cfh2607.phone

data class GlucoseRecord(
        val timestamp: String,
        val fingerstickGlucose: Int,
        val ppgCount: Int,
        val ppgRawData: String
)
