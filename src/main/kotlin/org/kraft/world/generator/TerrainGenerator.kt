package org.kraft.world.generator

import org.kraft.world.Chunk

interface TerrainGenerator {
    fun generate(chunk: Chunk)
}
