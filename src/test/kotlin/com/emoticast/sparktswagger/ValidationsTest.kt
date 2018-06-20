package com.emoticast.sparktswagger

import com.emoticast.extensions.json
import org.junit.Rule
import org.junit.Test

class ValidationsTest {

    @Rule
    @JvmField val rule = SparkTestRule()

    @Test
    fun `validates routes`() {

        whenPerform GET "/" expectCode 200
        whenPerform GET "/home/v1/clips/3456" expectCode 200
        whenPerform GET "/home/v1/clips/hey" expectCode 400 expectBody ClientError(400, listOf("""Path parameter `clipId` is invalid, expecting non negative integer, got `hey`""")).json
        whenPerform GET "/v1/clips/134?offset=-34" expectCode 400 expectBody ClientError(400, listOf("""Query parameter `offset` is invalid, expecting non negative integer, got `-34`""")).json
    }
}