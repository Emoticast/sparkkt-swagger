package com.emoticast.sparktswagger

import com.emoticast.sparktswagger.documentation.Description
import com.emoticast.sparktswagger.documentation.Visibility
import com.emoticast.sparktswagger.extensions.json
import org.junit.Rule
import org.junit.Test

class SimplePathBuilderTest : SparkTest() {

    @Rule
    @JvmField
    val rule = SparkTestRule(port) {
        http GET "foo" inSummary
                "returns a foo" isDescribedAs "" isHandledBy
                { TestResult("get value").ok }

        "foo" PUT "/foo" isHandledBy { TestResult("put value").created }
        "foo" POST "/foo" isHandledBy { TestResult("post value").created }
        "foo" DELETE "/foo" isHandledBy { TestResult("delete value").ok }

        "foo" GET "/error" isHandledBy { if (false) TestResult("never happens").ok else badRequest("Something went wrong") }
        "foo" GET "/forbidden" isHandledBy { if (false) TestResult("never happens").ok else forbidden("Forbidden") }

        "foo" GET "noslash/bar" isHandledBy { TestResult("success").ok }
        "foo" PUT "noslash/bar" isHandledBy { TestResult("success").ok }
        "foo" POST "noslash/bar" isHandledBy { TestResult("success").ok }
        "foo" DELETE "noslash/bar" isHandledBy { TestResult("success").ok }

        "foo" GET "infixslash" / "bar" isHandledBy { TestResult("success").ok }
        "foo" PUT "infixslash" / "bar" isHandledBy { TestResult("success").ok }
        "foo" POST "infixslash" / "bar" isHandledBy { TestResult("success").ok }
        "foo" DELETE "infixslash" / "bar" isHandledBy { TestResult("success").ok }

        "one" / {
            "foo" GET "/a" isHandledBy { TestResult("get value").ok }
            "foo" GET "/b" isHandledBy { TestResult("get value").ok }
            "two" / {
                "foo" GET "/c" isHandledBy { TestResult("get value").ok }
            }
        }

        "hey" / "there" / {
            "foo" GET "/a" isHandledBy { TestResult("get value").ok }
        }

        "hey" / {
            clipId / {
                "foo" GET "/a" isHandledBy { TestResult("get value").ok }
            }
        }

        "v1" / {
            GET() isHandledBy { TestResult("get value").ok }
            http GET clipId isHandledBy { TestResult("get value").ok }
            "foo" GET "one" / clipId isHandledBy { TestResult("get value").ok }
        }

        GET("params1" / clipId / "params2" / otherPathParam) isHandledBy { TestResult("${request[clipId]}${request[otherPathParam]}").ok }

//        "params3" / {
//            clipId / {
//                "params4" / {
//                    otherPathParam / {
//                        GET() isHandledBy { TestResult("${request[clipId]}${request[otherPathParam]}").ok }
//                    }
//                }
//            }
//        }


        GET() isHandledBy { TestResult("get value").ok }
    }


    @Test
    fun `supports nested routes`() {
        whenPerform GET "/$root/one/a" expectBodyJson TestResult("get value") expectCode 200
        whenPerform GET "/$root/one/b" expectBodyJson TestResult("get value") expectCode 200
        whenPerform GET "/$root/one/two/c" expectBodyJson TestResult("get value") expectCode 200
        whenPerform GET "/$root/hey/there/a" expectBodyJson TestResult("get value") expectCode 200

        whenPerform GET "/$root/hey/a" expectCode 404
        whenPerform GET "/$root/hey/123/a" expectBodyJson TestResult("get value") expectCode 200
        whenPerform GET "/$root/v1/123" expectBodyJson TestResult("get value") expectCode 200
        whenPerform GET "/$root/v1/one/123" expectBodyJson TestResult("get value") expectCode 200
        whenPerform GET "/$root/v1" expectBody TestResult("get value").json expectCode 200
        whenPerform GET "/$root" expectBody TestResult("get value").json expectCode 200
    }

    @Test
    fun `returns successful status codes`() {
        whenPerform GET "/$root/foo" expectBodyJson TestResult("get value") expectCode 200
        whenPerform PUT "/$root/foo" expectBodyJson TestResult("put value") expectCode 201
        whenPerform POST "/$root/foo" expectBodyJson TestResult("post value") expectCode 201
        whenPerform DELETE "/$root/foo" expectBodyJson TestResult("delete value") expectCode 200
    }

    @Test
    fun `returns error responses`() {
        whenPerform GET "/$root/error" expectBodyJson badRequest<TestResult, String>("Something went wrong") expectCode 400
        whenPerform GET "/$root/forbidden" expectBodyJson badRequest<TestResult, String>("Forbidden", 403) expectCode 403
    }

    @Test
    fun `when there's no leading slash, it adds it`() {
        whenPerform GET "/$root/noslash/bar" expectBodyJson TestResult("success") expectCode 200
        whenPerform PUT "/$root/noslash/bar" expectBodyJson TestResult("success") expectCode 200
        whenPerform POST "/$root/noslash/bar" expectBodyJson TestResult("success") expectCode 200
        whenPerform DELETE "/$root/noslash/bar" expectBodyJson TestResult("success") expectCode 200
    }

    @Test
    fun `supports infix slash`() {
        whenPerform GET "/$root/infixslash/bar" expectBodyJson TestResult("success") expectCode 200
        whenPerform PUT "/$root/infixslash/bar" expectBodyJson TestResult("success") expectCode 200
        whenPerform POST "/$root/infixslash/bar" expectBodyJson TestResult("success") expectCode 200
        whenPerform DELETE "/$root/infixslash/bar" expectBodyJson TestResult("success") expectCode 200
    }

    @Test
    fun `supports several path parameters`() {
        whenPerform GET "/$root/params1/90/params2/32" expectBodyJson TestResult("9032") expectCode 200
//        whenPerform GET "/$root/params3/42/params4/24" expectBodyJson TestResult("4224") expectCode 200
    }
}

data class TestResult(@Description(visibility = Visibility.INTERNAL) val value: String)