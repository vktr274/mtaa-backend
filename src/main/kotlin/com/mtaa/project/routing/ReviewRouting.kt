package com.mtaa.project.routing

import com.mtaa.project.*
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.NullPointerException

fun Route.reviewRouting() {
    route("/reviews") {
        get("{id}") {
            val id = parseInt(call,"id")
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }

            val reviewInfo = getReviewInfoData(id)
            if(reviewInfo == null){
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            // Review found
            call.respond(reviewInfo)
        }

        post {
            val auth = getID(call)
            if (auth == -1) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }

            try {
                val data = call.receive<ReviewPostInfo>()

                if (data.attributes.equals(null)) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }

                val result = transaction {
                    createReview(data, auth)
                }
                when (result) {
                    Status.UNAUTHORIZED -> {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@post
                    }
                    Status.NOT_FOUND -> {
                        call.respond(HttpStatusCode.NotFound)
                        return@post
                    }
                    Status.OK -> {
                        call.respond(HttpStatusCode.OK)
                        return@post
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    //Got null payload
                    is ContentTransformationException -> call.respond(HttpStatusCode.BadRequest)
                    //Some values missing in payload
                    is NullPointerException -> call.respond(HttpStatusCode.BadRequest)
                    else -> {
                        println(e.stackTraceToString())
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }
            }
        }
        put("{id}") {
            val auth = getID(call)
            if (auth == -1) {
                call.respond(HttpStatusCode.Unauthorized)
                return@put
            }

            val id = parseInt(call,"id")
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@put
            }

            try {
                val data = call.receive<ReviewPutInfo>()

                if (data.attributes.equals(null)) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@put
                }

                val result = transaction {
                    updateReview(data, id, auth)
                }
                when (result) {
                    Status.UNAUTHORIZED -> {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@put
                    }
                    Status.NOT_FOUND -> {
                        call.respond(HttpStatusCode.NotFound)
                        return@put
                    }
                    Status.OK -> {
                        call.respond(HttpStatusCode.OK)
                        return@put
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    //Got null payload
                    is ContentTransformationException -> call.respond(HttpStatusCode.BadRequest)
                    //Some values missing in payload
                    is NullPointerException -> call.respond(HttpStatusCode.BadRequest)
                    else -> {
                        println(e.stackTraceToString())
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }
            }
        }
        put("{id}/like") {
            val id = parseInt(call,"id")
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@put
            }

            val auth = getID(call)
            if (auth == -1) {
                call.respond(HttpStatusCode.Unauthorized)
                return@put
            }

            val result = transaction {
                voteOnReview(auth, id, true)
            }
            when (result) {
                Status.UNAUTHORIZED -> {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@put
                }
                Status.NOT_FOUND -> {
                    call.respond(HttpStatusCode.NotFound)
                    return@put
                }
                Status.OK -> {
                    call.respond(HttpStatusCode.OK)
                    return@put
                }
            }
        }
        put("{id}/dislike") {
            val id = parseInt(call,"id")
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@put
            }

            val auth = getID(call)
            if (auth == -1) {
                call.respond(HttpStatusCode.Unauthorized)
                return@put
            }

            val result = transaction {
                voteOnReview(auth, id, false)
            }
            when (result) {
                Status.UNAUTHORIZED -> {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@put
                }
                Status.NOT_FOUND -> {
                    call.respond(HttpStatusCode.NotFound)
                    return@put
                }
                Status.OK -> {
                    call.respond(HttpStatusCode.OK)
                    return@put
                }
            }
        }
        delete("{id}") {
            val id = parseInt(call,"id")
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@delete
            }

            val auth = getID(call)
            if (auth == -1) {
                call.respond(HttpStatusCode.Unauthorized)
                return@delete
            }

            val result = transaction {
                deleteReview(auth, id)
            }
            when (result) {
                Status.UNAUTHORIZED -> {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@delete
                }
                Status.NOT_FOUND -> {
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }
                Status.OK -> {
                    call.respond(HttpStatusCode.OK)
                    return@delete
                }
            }
        }
    }
}

fun getReviewInfoData(id:Int): ReviewInfo? {
    val review = transaction {
        getReviewInfo(id)
    } ?: return null
    val photos = transaction {
        getPhotos(id)
    }
    val attributes = transaction {
        getReviewAttributes(id)
    }
    val votes = transaction {
        getReviewVotes(id)
    }
    val photosInfo: MutableList<PhotoInfo> = mutableListOf()
    for (photo in photos) {
        photosInfo.add(PhotoInfo(photo.id.toString().toInt()))
    }

    val attributesInfo: MutableList<ReviewAttributeInfo> = mutableListOf()
    for (attribute in attributes) {
        attributesInfo.add(ReviewAttributeInfo(attribute.text, attribute.is_positive, id))
    }

    var likes = 0
    var dislikes = 0
    for (vote in votes) {
        if (vote.is_positive) {
            likes++
        } else {
            dislikes++
        }
    }

    var product_id = 0
    var user_id = 0
    transaction {
        product_id = review.product.id.toString().toInt()
        user_id = review.user.id.toString().toInt()
    }

    // Review found
    return ReviewInfo(
        review.text, attributesInfo, photosInfo,
        likes, dislikes, product_id, review.score,
        user_id, review.created_at.toString()
    )
}
