package com.deelib.perfScout.frame

data class FrameRenderingInfo(
    val totalFrames: Int,
    val droppedFrames: Int,
    val slowFrames: Int,
    val averageFrameTime: Double,
    val jankPercentage: Double,
    val isSmooth: Boolean,
    val performanceScore: Int
) 